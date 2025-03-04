<template>

  <el-dialog :close-on-click-modal="false" :title="$t('operating_log.change_history')" :visible.sync="infoVisible"
             width="900px" :destroy-on-close="true" append-to-body
             @close="handleClose" v-loading="loading">
    <el-table :data="details">
      <el-table-column prop="operTime" :label="$t('operating_log.time')">
        <template v-slot:default="scope">
          <span>{{ scope.row.operTime | datetimeFormat }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="userName" :label="$t('operating_log.user')"/>
      <el-table-column prop="columnTitle" :label="$t('operating_log.change_field')" v-if="showChangeField">
        <template v-slot:default="scope">
          <div v-if="scope.row.details && scope.row.details.columns">
            <div v-for="detail in scope.row.details.columns" :key="detail.id">{{ detail.columnTitle }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="originalValue" :label="$t('operating_log.before_change')">
        <template v-slot:default="scope">
          <div v-if="scope.row.details && scope.row.details.columns">
            <div v-for="detail in scope.row.details.columns" :key="detail.id">
              <div v-if="linkDatas.indexOf(detail.columnName)!== -1">
                <el-link style="color: #409EFF" @click="openDetail(scope.row,detail)">{{
                    $t('operating_log.info')
                  }}
                </el-link>
              </div>
              <el-tooltip :content="detail.originalValue" v-else>
                <div class="current-value">{{ detail.originalValue ? detail.originalValue : "空值" }}</div>
              </el-tooltip>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="newValue" :label="$t('operating_log.after_change')">
        <template v-slot:default="scope">
          <div v-if="scope.row.details && scope.row.details.columns">
            <div v-for="detail in scope.row.details.columns" :key="detail.id">
              <div v-if="linkDatas.indexOf(detail.columnName)!== -1">
                <el-link style="color: #409EFF" @click="openDetail(scope.row,detail)">{{
                    $t('operating_log.info')
                  }}
                </el-link>
              </div>
              <el-tooltip :content="detail.newValue" v-else>
                <div class="current-value">{{ detail.newValue ? detail.newValue : "空值" }}</div>
              </el-tooltip>
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <ms-table-pagination :change="getDetails" :current-page.sync="goPage" :page-size.sync="pageSize"
                         :total="totalCount"/>
    <ms-environment-history-detail ref="environmentHistoryDetail"/>
  </el-dialog>
</template>
<script>
import MsEnvironmentHistoryDetail from "./EnvironmentHistoryDetail";
import {getOperatingLogForResource} from "../../../api/history";


export default {
  name: "MsEnvHistory",
  components: {
    MsEnvironmentHistoryDetail,
    MsTablePagination: () => import("../../pagination/TablePagination"),
  },
  props: {
    title: String,
  },
  data() {
    return {
      infoVisible: false,
      loading: false,
      details: [],
      linkDatas: ["prerequisite", "steps", "remark", "request", "config",
        "response", "scenarioDefinition", "tags", "loadConfiguration", "advancedConfiguration"],
      showChangeField: true,
      pageSize: 10,
      goPage: 1,
      totalCount: 0,
      id: String,
      module: String
    }
  },
  methods: {
    handleClose() {
      this.infoVisible = false;
    },
    getDetails(id, modules) {
      id = this.id;
      modules = this.module;
      this.loading = true;
      getOperatingLogForResource(this.goPage, this.pageSize, {sourceId: id, modules: modules})
        .then(response => {
          this.loading = false;
          let data = response.data.listObject;
          this.totalCount = response.data.itemCount;
          if (data) {
            //过滤接口定义请求参数为空的数据
            if (modules.length > 0 && modules[0] === '接口定义') {
              for (let i = 0; i < data.length; i++) {
                if (data[i].details.columns && data[i].details.columns.length > 0) {
                  let columns = data[i].details.columns;
                  for (let j = 0; j < columns.length; j++) {
                    if (columns[j].columnName === 'request' && (columns[j].diffValue === null || columns[j].diffValue === '')) {
                      data[i].details.columns.splice(j, 1);
                    }
                  }
                }
              }
            }
            // 过滤非全局脚本历史变更数据
            if (modules.length > 0 && modules[0] === '项目-环境设置') {
              // 环境设置不显示变更字段
              this.showChangeField = false;
              // 不显示的节点 id
              let ids = [];
              for (let i = 0; i < data.length; i++) {
                if (data[i].details.columns.findIndex(d => (d.diffValue === null || d.diffValue === '')) !== -1) {
                  ids.push(data[i].id);
                  continue;
                }
              }
              if (ids.length > 0) {
                ids.forEach(row => {
                  const index = data.findIndex(d => d.id === row);
                  data.splice(index, 1);
                });
              }
            } else {
              this.showChangeField = true;
            }
            this.details = data;
          }
        })
    },
    open(id, modules) {
      this.infoVisible = true;
      this.id = id;
      this.module = modules;
      this.getDetails(id, modules);
    },
    openDetail(row, value) {
      value.createUser = row.details.createUser;
      value.operTime = row.operTime;
      if (value.columnName === "tags") {
        this.$refs.tagsHistoryDetail.open(value);
      } else if ((value.columnName === "request" || value.columnName === "response") &&
        (row.operModule === "接口定义" || row.operModule === "接口定義" || row.operModule === "Api definition" || row.operModule === "API_DEFINITION")) {
        this.$refs.apiHistoryDetail.open(value);
      } else if (row.operModule === "项目-环境设置" || row.operModule === "項目-環境設置" || row.operModule === "PROJECT_ENVIRONMENT_SETTING") {
        this.$refs.environmentHistoryDetail.open(value);
      } else {
        try {
          value.newValue = JSON.parse(value.newValue);
          value.originalValue = JSON.parse(value.originalValue);
        } catch (e) {
          // console.info(e);
        }
        this.$refs.historyDetail.open(value);
      }
    },
  }
}
</script>

<style scoped>
.current-value {
  display: inline-block;
  overflow-x: hidden;
  padding-bottom: 0;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
  width: 120px;
}

</style>
