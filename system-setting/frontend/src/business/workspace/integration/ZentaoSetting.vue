<template>
  <div>
    <div style="width: 500px">
      <div style="margin-top: 20px;margin-bottom: 10px">{{ $t('organization.integration.basic_auth_info') }}</div>
      <el-form :model="form" ref="form" label-width="100px" size="small" :disabled="show" :rules="rules">
        <el-form-item :label="$t('organization.integration.account')" prop="account">
          <el-input v-model="form.account" :placeholder="$t('organization.integration.input_api_account')"/>
        </el-form-item>
        <el-form-item :label="$t('organization.integration.password')" prop="password">
          <el-input v-model="form.password" auto-complete="new-password" v-if="showInput"
                    :placeholder="$t('organization.integration.input_api_password')" show-password/>
        </el-form-item>
        <el-form-item :label="$t('organization.integration.zentao_url')" prop="url">
          <el-input v-model="form.url" :placeholder="$t('organization.integration.input_zentao_url')"/>
        </el-form-item>
        <el-form-item :label="$t('organization.integration.zentao_request')" prop="request">
          <el-radio v-model="form.request" label="PATH_INFO" size="small" border> PATH_INFO</el-radio>
          <el-radio v-model="form.request" label="GET" size="small" border>GET</el-radio>
          <ms-instructions-icon effect="light" style="margin-left: -20px;">
            {{ $t('organization.integration.zentao_config_tip')}} <br/><br/>
            {{ $t('organization.integration.zentao_config_path')}}
          </ms-instructions-icon>
        </el-form-item>
      </el-form>
    </div>

    <bug-manage-btn @save="save"
                    @init="init"
                    :edit-permission="['WORKSPACE_SERVICE:READ+EDIT']"
                    @testConnection="testConnection"
                    @cancelIntegration="cancelIntegration"
                    @reloadPassInput="reloadPassInput"
                    :form="form"
                    :show.sync="show"
                    ref="bugBtn"/>

    <div class="defect-tip">
      <div>{{ $t('organization.integration.use_tip') }}</div>
      <div>
        1. {{ $t('organization.integration.use_tip_zentao') }}
      </div>
      <div>
        2. {{ $t('organization.integration.use_tip_two') }}
        <router-link to="/setting/project/all" style="margin-left: 5px;color: #551A8B; text-decoration: underline; cursor: pointer">
          {{ $t('organization.integration.link_the_project_now') }}
        </router-link>
      </div>
      <div>
        3. {{ $t('organization.integration.use_tip_three') }}
        <span  style="margin-left: 5px;color: #551A8B; text-decoration: underline; cursor: pointer" @click="resVisible = true">
          {{ $t('organization.integration.link_the_info_now') }}
        </span>
        <el-dialog :close-on-click-modal="false" width="80%"
                   :visible.sync="resVisible" destroy-on-close @close="closeDialog">
          <ms-person-router @closeDialog = "closeDialog"/>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<script>
import BugManageBtn from "./BugManageBtn";
import {getCurrentUser, getCurrentWorkspaceId} from "metersphere-frontend/src/utils/token";
import {ZEN_TAO} from "metersphere-frontend/src/utils/constants";
import MsInstructionsIcon from "metersphere-frontend/src/components/MsInstructionsIcon";
import MsPersonRouter from "metersphere-frontend/src/components/personal/PersonRouter";
import {
  authServiceIntegration,
  delServiceIntegration,
  getServiceIntegration,
  saveServiceIntegration
} from "../../../api/workspace";

export default {
  name: "ZentaoSetting",
  components: {
    MsInstructionsIcon,
    BugManageBtn,
    MsPersonRouter
  },
  created() {
    this.init();
  },
  data() {
    return {
      show: true,
      showInput: true,
      resVisible:false,
      form: {},
      rules: {
        account: {
          required: true,
          message: this.$t('organization.integration.input_api_account'),
          trigger: ['change', 'blur']
        },
        password: {
          required: true,
          message: this.$t('organization.integration.input_api_password'),
          trigger: ['change', 'blur']
        },
        url: {
          required: true,
          message: this.$t('organization.integration.input_zentao_url'),
          trigger: ['change', 'blur']
        },
        request: {
          required: true,
          message: this.$t('organization.integration.input_zentao_request'),
          trigger: ['change', 'blur']
        },
      },
    };
  },
  methods: {
    save() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          let formatUrl = this.form.url.trim();
          if (!formatUrl.endsWith('/')) {
            formatUrl = formatUrl + '/';
          }
          const {lastWorkspaceId} = getCurrentUser();
          let param = {};
          let auth = {
            account: this.form.account,
            password: this.form.password,
            url: formatUrl,
            request: this.form.request
          };
          param.workspaceId = lastWorkspaceId;
          param.platform = ZEN_TAO;
          param.configuration = JSON.stringify(auth);
          this.$parent.loading = saveServiceIntegration(param).then(() => {
            this.show = true;
            this.$refs.bugBtn.showEdit = true;
            this.$refs.bugBtn.showSave = false;
            this.$refs.bugBtn.showCancel = false;
            this.reloadPassInput();
            this.init();
            this.$success(this.$t('commons.save_success'));
          });
        } else {
          return false;
        }
      });
    },
    init() {
      const {lastWorkspaceId} = getCurrentUser();
      let param = {};
      param.platform = ZEN_TAO;
      param.workspaceId = lastWorkspaceId;
      this.$parent.loading = getServiceIntegration(param).then(res => {
        let data = res.data;
        if (data.configuration) {
          let config = JSON.parse(data.configuration);
          this.$set(this.form, 'account', config.account);
          this.$set(this.form, 'password', config.password);
          this.$set(this.form, 'url', config.url);
          this.$set(this.form, 'request', config.request ? config.request : 'PATH_INFO');
        } else {
          this.clear();
        }
      });
    },
    clear() {
      this.$set(this.form, 'account', '');
      this.$set(this.form, 'password', '');
      this.$set(this.form, 'url', '');
      this.$set(this.form, 'request', '');
      this.$nextTick(() => {
        if (this.$refs.form) {
          this.$refs.form.clearValidate();
        }
      });
    },
    testConnection() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.account && this.form.password) {
            this.$parent.loading = authServiceIntegration(getCurrentWorkspaceId(), ZEN_TAO).then(() => {
              this.$success(this.$t('organization.integration.verified'));
            });
          } else {
            this.$warning(this.$t('organization.integration.not_integrated'));
            return false;
          }
        } else {
          return false;
        }
      });
    },
    cancelIntegration() {
      if (this.form.account && this.form.password) {
        this.$alert(this.$t('organization.integration.cancel_confirm') + ZEN_TAO + "？", '', {
          confirmButtonText: this.$t('commons.confirm'),
          callback: (action) => {
            if (action === 'confirm') {
              const {lastWorkspaceId} = getCurrentUser();
              let param = {};
              param.workspaceId = lastWorkspaceId;
              param.platform = ZEN_TAO;
              this.$parent.loading = delServiceIntegration(param).then(() => {
                this.$success(this.$t('organization.integration.successful_operation'));
                this.init('');
              });
            }
          }
        });
      } else {
        this.$warning(this.$t('organization.integration.not_integrated'));
      }
    },
    reloadPassInput() {
      this.showInput = false;
      this.$nextTick(function () {
        this.showInput = true;
      });
    },
    closeDialog(){
      this.resVisible = false;
    }
  }
};
</script>

<style scoped>
.defect-tip {
  background: #EDEDED;
  border: solid #E1E1E1 1px;
  margin: 10px 0;
  padding: 10px;
  border-radius: 3px;
}
</style>
