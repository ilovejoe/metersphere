<template>
  <div class="card-container">
    <el-card class="card-content" v-loading="mockConfigData.loading">
      <p class="tip">期望列表</p>
      <div class="card">
        <el-input :placeholder="$t('commons.search_by_name')" class="search-input" size="small"
                  :clearable="serchInputClearable"
                  v-model="tableSearch"/>
        <el-table ref="table" border
                  :data="mockConfigData.mockExpectConfigList.filter(data=>!tableSearch || data.name.toLowerCase().includes(tableSearch.toLowerCase()))"
                  @row-click="clickRow"
                  row-key="id" class="test-content" :height="screenHeight">
          <el-table-column :label="$t('api_test.mock.table.name')" min-width="160px" prop="name"></el-table-column>
          <el-table-column :label="$t('api_test.mock.table.tag')" min-width="200px" prop="tags">
            <template v-slot:default="scope">
              <ms-tag v-for="(itemName,index)  in scope.row.tags" :key="index" type="success" effect="plain"
                      :show-tooltip="true" :content="itemName"
                      style="margin-left: 0px; margin-right: 2px"/>
            </template>
          </el-table-column>
          <el-table-column :label="$t('api_test.mock.table.creator')" min-width="160px"
                           prop="createUserId"></el-table-column>
          <el-table-column :label="$t('api_test.mock.table.status')" min-width="80px" prop="status">
            <template v-slot:default="scope">
              <div>
                <el-switch
                  v-model="scope.row.status"
                  class="captcha-img"
                  @change="changeStatus(scope.row)"
                ></el-switch>
              </div>
            </template>
          </el-table-column>
          <el-table-column :label="$t('api_test.mock.table.update_time')" min-width="160px" prop="updateTime">
            <template v-slot:default="scope">
              <span>{{ scope.row.updateTime | datetimeFormat }}</span>
            </template>
          </el-table-column>
          <el-table-column fixed="right" min-width="100" align="center" :label="$t('commons.operating')">
            <template v-slot:default="scope">
              <div>
                <ms-table-operator-button :tip="$t('commons.copy')" icon="el-icon-copy-document"
                                          @exec="copyExpect(scope.row)"
                />
                <ms-table-operator-button :tip="$t('commons.delete')" icon="el-icon-delete"
                                          @exec="removeExpect(scope.row)"
                />
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!--  期望详情 -->
      <p class="tip">{{ $t('api_test.mock.expect_detail') }}</p>
      <el-form :model="mockExpectConfig" :rules="rule" ref="mockExpectForm" label-width="80px" label-position="right">
        <div class="card">
          <div class="base-info">
            <el-row>
              <el-col>{{ $t('api_test.mock.base_info') }}</el-col>
            </el-row>
            <el-row>
              <el-col :span="12">
                <el-form-item :label="$t('commons.name')" prop="name">
                  <el-input class="ms-http-input" size="small" v-model="mockExpectConfig.name"/>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item :label="$t('commons.tag')" prop="tag">
                  <ms-input-tag :currentScenario="mockExpectConfig" v-if="showHeadTable" ref="tag"/>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row>
              <el-col>{{ $t('api_test.mock.req_param') }}</el-col>
            </el-row>
            <el-row>
              <tcp-params :request="mockExpectConfig.request" style="margin: 10px 10px;" ref="tcpParam"></tcp-params>
            </el-row>
            <el-row style="margin-top: 10px;">
              <el-col>{{ $t('api_test.mock.rsp_param') }}</el-col>
            </el-row>
            <el-row>
              <el-form-item label="延时 (ms)" prop="response.delayed">
                <el-input-number v-model="mockExpectConfig.response.delayed" :min="0">
                  <template slot="append">ms</template>
                </el-input-number>
              </el-form-item>
            </el-row>
            <el-row style="margin-top: 10px;">
              <el-form-item label="Body:" label-width="50px">
                <ms-code-edit height="200px" :mode="'txt'" ref="codeEdit" :data.sync="mockExpectConfig.response.body"
                              style="margin-top: 10px;"/>
              </el-form-item>
            </el-row>
            <el-row>
              <div style="float: right;margin-right: 20px">
                <el-button type="primary" size="small" @click="saveMockExpectConfig" title="ctrl + s">{{
                    $t('commons.add')
                  }}
                </el-button>
                <el-button type="primary" size="small" @click="cleanMockExpectConfig">{{
                    $t('commons.clear')
                  }}
                </el-button>
                <el-button type="primary" size="small" v-if="mockExpectConfig.id != '' && mockExpectConfig.id != null"
                           @click="updateMockExpectConfig">{{ $t('commons.update') }}
                </el-button>
              </div>
            </el-row>
          </div>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import {createMockConfig, delMock, mockExpectConfig, updateMockExpectConfig} from "@/api/api-mock";
import MsTableOperatorButton from "metersphere-frontend/src/components/MsTableOperatorButton";
import MockRowVariables from "@/business/definition/components/mock/MockRowVariables";
import MsInputTag from "@/business/automation/scenario/MsInputTag";
import MsCodeEdit from "@/business/definition/components/MsCodeEdit";
import MsApiVariableAdvance from "metersphere-frontend/src/components/environment/commons/ApiVariableAdvance";
import MsTag from "metersphere-frontend/src/components/MsTag";
import {REQUEST_HEADERS} from "metersphere-frontend/src/utils/constants";
import TcpParams from "@/business/definition/components/request/tcp/TcpParams";
import {operationConfirm} from "metersphere-frontend/src/utils";


export default {
  name: "TcpMockConfig",
  components: {
    MockRowVariables,
    MsTableOperatorButton,
    MsInputTag,
    MsCodeEdit,
    MsApiVariableAdvance,
    TcpParams,
    MsTag,
  },
  data() {
    return {
      screenHeight: 300,
      tableSearch: "",
      showHeadTable: true,
      serchInputClearable: true,
      mockConfigData: {},
      apiParams: [],
      headerSuggestions: REQUEST_HEADERS,
      mockExpectConfig: {
        id: "",
        name: "",
        mockConfigId: "",
        request: {
          reportType: 'raw',
          xmlDataStruct: [],
          jsonDataStruct: "",
          rawDataStruct: "",
        },
        response: {
          httpCode: "",
          delayed: "",
          httpHeads: [],
          body: "",
        },
      },
      rule: {
        name: [
          {required: true, message: this.$t('test_track.case.input_name'), trigger: 'blur'},
          {max: 100, message: this.$t('test_track.length_less_than') + '100', trigger: 'blur'}
        ],
        response: {
          httpCode: [{required: true, message: this.$t('api_test.mock.rule.input_code'), trigger: 'blur'},],
          delayed: [{required: true, message: this.$t('test_track.case.input_name'), trigger: 'blur'},],
        },
      },
    };
  },
  props: {
    baseMockConfigData: {},
    type: String
  },
  created() {
    this.mockConfigData = this.baseMockConfigData;
    // this.searchApiParams(this.mockConfigData.mockConfig.apiId);
  },
  methods: {
    copyExpect(row) {
      mockExpectConfig(row.id).then(response => {
        let data = response.data;
        this.showHeadTable = false;
        this.mockExpectConfig = data;
        this.mockExpectConfig.id = "";
        this.mockExpectConfig.name = this.mockExpectConfig.name + "_copy";
        if (this.mockExpectConfig.request == null) {
          this.mockExpectConfig.request = {
            jsonParam: false,
            variables: [],
            jsonData: "{}",
          };
        }
        if (this.mockExpectConfig.response == null) {
          this.mockExpectConfig.response = {
            httpCode: "",
            delayed: "",
            httpHeads: [],
            body: "",
          };
        }
        this.$nextTick(function () {
          this.showHeadTable = true;
          this.saveMockExpectConfig();
        });
      });
    },
    removeExpect(row) {
      operationConfirm(this, this.$t('api_test.mock.delete_mock_expect'), () => {
        let mockInfoId = row.mockConfigId;
        delMock(row.id).then(response => {
          this.cleanMockExpectConfig();
          this.refreshMockInfo(mockInfoId);
          this.$message({
            type: 'success',
            message: this.$t('commons.delete_success'),
          });
        });
      });
    },
    saveMockExpectConfig() {
      let mockConfigId = this.mockConfigData.mockConfig.id;
      this.mockExpectConfig.mockConfigId = mockConfigId;
      this.mockExpectConfig.id = "";
      let formCheckResult = this.checkMockExpectForm("mockExpectForm", true);
    },
    cleanMockExpectConfig() {
      this.showHeadTable = false;
      this.mockExpectConfig = {
        id: "",
        name: "",
        mockConfigId: "",
        request: {
          reportType: 'raw',
          xmlDataStruct: [],
          jsonDataStruct: "",
          rawDataStruct: "",
        },
        response: {
          httpCode: "",
          delayed: "",
          httpHeads: [],
          body: "",
        },
      };
      this.$nextTick(function () {
        this.$refs.tcpParam.reload();
        this.showHeadTable = true;
      });
    },
    updateMockExpectConfig() {
      this.checkMockExpectForm("mockExpectForm");
    },
    uploadMockExpectConfig(clearForm) {
      let param = this.mockExpectConfig;
      updateMockExpectConfig(param).then(response => {
        let returnData = response.data;
        this.mockExpectConfig.id = returnData.id;
        this.refreshMockInfo(param.mockConfigId);
        if (clearForm) {
          this.cleanMockExpectConfig();
        }
        this.$message({
          type: 'success',
          message: this.$t('commons.save_success'),
        });
      });
    },
    refreshMockInfo(mockConfigId) {
      let mockParam = {};
      mockParam.id = mockConfigId;
      createMockConfig(mockParam).then(response => {
        this.mockConfigData = response.data;
      });
    },
    checkMockExpectForm(formName, clearForm) {
      this.$refs[formName].validate((valid) => {
        if (valid) {
          this.uploadMockExpectConfig(clearForm);
          return true;
        } else {
          return false;
        }
      });
    },
    changeStatus(row) {
      let mockParam = {};
      mockParam.id = row.id;
      mockParam.status = row.status;
      updateMockExpectConfig(mockParam).then(response => {
      });
    },
    clickRow(row, column, event) {
      this.cleanMockExpectConfig();
      mockExpectConfig(row.id).then(response => {
        let data = response.data;
        this.showHeadTable = false;
        this.mockExpectConfig = data;
        if (this.mockExpectConfig.request == null) {
          this.mockExpectConfig.request = {
            reportType: 'raw',
            xmlDataStruct: [],
          };
        }
        if (this.mockExpectConfig.response == null) {
          this.mockExpectConfig.response = {
            httpCode: "",
            delayed: "",
            httpHeads: [],
            body: "",
          };
        }
        this.$nextTick(function () {
          this.$refs.tcpParam.reload();
          this.showHeadTable = true;
        });
      });
    }
  }
};
</script>

<style scoped>
.search-input {
  float: right;
  width: 300px;
  margin-right: 10px;
  margin-bottom: 10px;
}

.base-info .el-form-item {
  width: 100%;
}

.base-info .el-form-item :deep(.el-form-item__content) {
  width: 80%;
}

.base-info .ms-http-select {
  width: 100%;
}

</style>
