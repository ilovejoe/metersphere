<template>
  <div class="card-container">
    <el-card class="card-content" v-loading="loading">
      <template v-slot:header>
        <test-plan-case-list-header
          :project-id="getProjectId()"
          :condition="condition"
          :plan-id="planId"
          :plan-status="planStatus"
          @refresh="search"
          @relevanceCase="$emit('relevanceCase')"
          v-if="isPlanModel"/>
      </template>
      <ms-table
        v-loading="loading"
        :data="tableData"
        :condition="condition"
        :total="total"
        :page-size.sync="pageSize"
        :operators="operators"
        :screen-height="screenHeight"
        :batch-operators="buttons"
        @handlePageChange="initTable"
        :fields.sync="fields"
        :field-key="tableHeaderKey"
        @order="initTable"
        :row-order-group-id="planId"
        :row-order-func="editTestPlanApiCaseOrder"
        :enable-order-drag="enableOrderDrag"
        row-key="id"
        @filter="search"
        ref="table">
        <span v-for="(item) in fields" :key="item.key">
          <ms-table-column :field="item"
                           :fields-width="fieldsWidth"
                           sortable
                           label="ID"
                           prop="num"
                           min-width="80">
             <template v-slot:default="scope">
               <el-link @click="openApiById(scope.row)">
                  <span>
                    {{ scope.row.num }}
                  </span>
               </el-link>
            </template>
          </ms-table-column>

          <ms-table-column :field="item" :fields-width="fieldsWidth" prop="name" sortable min-width="120"
                           :label="$t('test_track.case.name')"/>

         <ms-table-column
           v-if="versionEnable"
           prop="versionId"
           :field="item"
           :filters="versionFilters"
           :fields-width="fieldsWidth"
           :label="$t('commons.version')"
           min-width="120px">
         <template v-slot:default="scope">
            <span>{{ scope.row.versionName }}</span>
          </template>
        </ms-table-column>

          <ms-table-column
            :field="item"
            :fields-width="fieldsWidth"
            prop="priority"
            :filters="priorityFilters"
            sortable
            :label="$t('test_track.case.priority')"
            min-width="120px">
            <template v-slot:default="scope">
              <priority-table-item :value="scope.row.priority"/>
            </template>
          </ms-table-column>

          <ms-table-column
            :field="item"
            :fields-width="fieldsWidth"
            min-width="100"
            prop="path"
            :label="$t('api_test.definition.api_path')"/>

          <ms-table-column
            :field="item"
            :fields-width="fieldsWidth"
            prop="createUser"
            sortable
            min-width="100"
            :filters="userFilters"
            :label="$t('commons.create_user')">
            <template v-slot:default="scope">
              {{ scope.row.creatorName }}
            </template>
          </ms-table-column>

          <ms-table-column
            :field="item"
            :fields-width="fieldsWidth"
            prop="environmentName"
            min-width="120"
            show-overflow-tooltip
            :label="$t('commons.environment')">
            <template v-slot:default="scope">
              {{ scope.row.environmentName || '-' }}
            </template>
          </ms-table-column>

          <ms-table-column
            v-if="item.id == 'maintainer'"
            prop="userId"
            :fields-width="fieldsWidth"
            :label="$t('custom_field.case_maintainer')"
            min-width="120">
             <template v-slot:default="scope">
              {{ scope.row.principalName }}
            </template>
          </ms-table-column>

          <ms-update-time-column :field="item" :fields-width="fieldsWidth"/>
          <ms-create-time-column :field="item" :fields-width="fieldsWidth"/>

          <ms-table-column
            :field="item"
            :fields-width="fieldsWidth"
            prop="tags"
            min-width="100"
            :label="$t('commons.tag')">
            <template v-slot:default="scope">
              <ms-tag v-for="(itemName,index)  in scope.row.tags" :key="index" type="success" effect="plain"
                      :content="itemName" style="margin-left: 0px; margin-right: 2px"/>
            </template>
          </ms-table-column>

          <ms-table-column :field="item"
                           prop="execResult"
                           :filters="execResultFilters"
                           :fields-width="fieldsWidth"
                           :label="$t('test_track.plan.execute_result')" min-width="150" align="center">
            <template v-slot:default="scope">
              <div v-loading="rowLoading === scope.row.id">
              <el-link @click="getReportResult(scope.row)"
                       :disabled="!scope.row.execResult || scope.row.execResult==='PENDING'">
                <ms-test-plan-api-status :status="scope.row.execResult"/>
              </el-link>
                <div v-if="scope.row.id" style="color: #999999;font-size: 12px">
                  <span> {{ scope.row.updateTime | datetimeFormat }}</span>
                  {{ scope.row.updateUser }}
                </div>
              </div>
            </template>
          </ms-table-column>
        </span>
      </ms-table>

      <ms-table-pagination :change="initTable" :current-page.sync="currentPage" :page-size.sync="pageSize"
                           :total="total"/>

      <test-plan-api-case-result ref="apiCaseResult"/>

      <!-- 批量编辑 -->
      <batch-edit :dialog-title="$t('test_track.case.batch_edit_case')" :type-arr="typeArr" :value-arr="valueArr"
                  :select-row="$refs.table ? $refs.table.selectRows : new Set()" ref="batchEdit"
                  @batchEdit="batchEdit"/>

      <ms-plan-run-mode
        :type="'apiCase'"
        :plan-case-ids="testPlanCaseIds"
        @close="search"
        @handleRunBatch="handleRunBatch"
        ref="runMode"/>
    </el-card>
    <ms-task-center ref="taskCenter" :show-menu="false"/>
  </div>

</template>

<script>

import MsTablePagination from "metersphere-frontend/src/components/pagination/TablePagination";
import MsTag from "metersphere-frontend/src/components/MsTag";
import MsContainer from "metersphere-frontend/src/components/MsContainer";
import MsBottomContainer from "metersphere-frontend/src/components/MsBottomContainer";
import BatchEdit from "@/business/case/components/BatchEdit";
import {API_METHOD_COLOUR, CASE_PRIORITY, RESULT_MAP} from "metersphere-frontend/src/model/JsonData";
import {getCurrentProjectID, getCurrentWorkspaceId} from "metersphere-frontend/src/utils/token";
import {hasLicense} from "metersphere-frontend/src/utils/permission";
import {getUUID, strMapToObj} from "metersphere-frontend/src/utils";
import PriorityTableItem from "../../../../common/tableItems/planview/PriorityTableItem";
import TestPlanCaseListHeader from "./TestPlanCaseListHeader";
import TestPlanApiCaseResult from "./TestPlanApiCaseResult";
import {TEST_PLAN_API_CASE} from "metersphere-frontend/src/utils/constants";
import {
  buildBatchParam,
  deepClone,
  getCustomTableHeader,
  getCustomTableWidth
} from "metersphere-frontend/src/utils/tableUtils";
import HeaderCustom from "metersphere-frontend/src/components/head/HeaderCustom";
import HeaderLabelOperate from "metersphere-frontend/src/components/head/HeaderLabelOperate";
import MsTaskCenter from "metersphere-frontend/src/components/task/TaskCenter";
import MsTable from "metersphere-frontend/src/components/table/MsTable";
import MsTableColumn from "metersphere-frontend/src/components/table/MsTableColumn";
import MsPlanRunMode from "@/business/plan/common/PlanRunModeWithEnv";
import MsUpdateTimeColumn from "metersphere-frontend/src/components/table/MsUpdateTimeColumn";
import MsCreateTimeColumn from "metersphere-frontend/src/components/table/MsCreateTimeColumn";
import {editTestPlanApiCaseOrder, reportSocket, run, testPlanAutoCheck} from "@/api/remote/plan/test-plan";
import {getProjectMemberUserFilter} from "@/api/user";
import {apiTestCaseGet, apiTestCaseReduction} from "@/api/remote/api/api-case";
import {
  testPlanApiCaseBatchDelete,
  testPlanApiCaseBatchUpdateEnv,
  testPlanApiCaseDelete,
  testPlanApiCaseList,
  testPlanApiCaseRun,
  testPlanApiCaseSelectAllTableRows
} from "@/api/remote/plan/test-plan-api-case";
import {apiDefinitionPlanReportGetByCaseId} from "@/api/remote/api/api-definition-report";
import MsTestPlanApiStatus from "@/business/plan/view/comonents/api/TestPlanApiStatus";
import {getProjectVersions} from "@/business/utils/sdk-utils";

export default {
  name: "TestPlanApiCaseList",
  components: {
    MsPlanRunMode,
    MsCreateTimeColumn,
    MsUpdateTimeColumn,
    MsTableColumn,
    MsTable,
    BatchEdit,
    HeaderLabelOperate,
    HeaderCustom,
    TestPlanApiCaseResult,
    TestPlanCaseListHeader,
    PriorityTableItem,
    MsTablePagination,
    MsTag,
    MsContainer,
    MsBottomContainer,
    MsTaskCenter,
    MsTestPlanApiStatus
  },
  mounted() {
    this.getVersionOptions();
  },
  data() {
    return {
      type: TEST_PLAN_API_CASE,
      tableHeaderKey: "TEST_PLAN_API_CASE",
      fields: getCustomTableHeader('TEST_PLAN_API_CASE'),
      fieldsWidth: getCustomTableWidth('TEST_PLAN_API_CASE'),
      tableLabel: [],
      condition: {},
      selectCase: {},
      loading: false,
      moduleId: "",
      status: 'default',
      deletePath: "/test/case/delete",
      enableOrderDrag: true,
      operators: [
        {
          tip: this.$t('api_test.run'), icon: "el-icon-video-play",
          exec: this.singleRun,
          class: this.planStatus === 'Archived' ? 'disable-run' : 'run-button',
          isDisable: this.planStatus === 'Archived',
          permissions: ['PROJECT_TRACK_PLAN:READ+RUN']
        },
        {
          tip: this.$t('test_track.plan_view.cancel_relevance'), icon: "el-icon-unlock",
          exec: this.handleDelete,
          type: 'danger',
          isDisable: this.planStatus === 'Archived',
          permissions: ['PROJECT_TRACK_PLAN:READ+RELEVANCE_OR_CANCEL']
        }
      ],
      buttons: [
        {
          name: this.$t('test_track.case.batch_unlink'),
          handleClick: this.handleDeleteBatch,
          isDisable: this.planStatus === 'Archived',
          permissions: ['PROJECT_TRACK_PLAN:READ+CASE_BATCH_DELETE']
        },
        {
          name: this.$t('api_test.automation.batch_execute'),
          handleClick: this.handleBatchExecute,
          isDisable: this.planStatus === 'Archived',
          permissions: ['PROJECT_TRACK_PLAN:READ+CASE_BATCH_RUN']
        },
        {
          name: this.$t('test_track.case.batch_edit_case'),
          handleClick: this.handleBatchEdit,
          isDisable: this.planStatus === 'Archived',
          permissions: ['PROJECT_TRACK_PLAN:READ+CASE_BATCH_EDIT']
        }
      ],
      typeArr: [
        {id: 'projectEnv', name: this.$t('api_test.definition.request.run_env')},
      ],
      priorityFilters: [
        {text: 'P0', value: 'P0'},
        {text: 'P1', value: 'P1'},
        {text: 'P2', value: 'P2'},
        {text: 'P3', value: 'P3'}
      ],
      valueArr: {
        priority: CASE_PRIORITY,
        userId: [],
        projectEnv: []
      },
      methodColorMap: new Map(API_METHOD_COLOUR),
      tableData: [],
      currentPage: 1,
      pageSize: 10,
      total: 0,
      selectDataCounts: 0,
      screenHeight: 'calc(100vh - 250px)',//屏幕高度
      currentCaseProjectId: "",
      runData: [],
      testPlanCaseIds: [],
      reportId: "",
      rowLoading: "",
      runningReport: new Set(),
      userFilters: [],
      projectIds: [],
      projectList: [],
      versionFilters: [],
      execResultFilters: [
        {text: 'Pending', value: 'PENDING'},
        {text: 'Success', value: 'SUCCESS'},
        {text: 'Error', value: 'ERROR'},
        {text: "FakeError", value: 'FAKE_ERROR'},
      ]
    };
  },
  props: {
    currentProtocol: String,
    selectNodeIds: Array,
    visible: {
      type: Boolean,
      default: false,
    },
    isApiListEnable: {
      type: Boolean,
      default: false,
    },
    isReadOnly: {
      type: Boolean,
      default: false
    },
    isCaseRelevance: {
      type: Boolean,
      default: false,
    },
    model: {
      type: String,
      default() {
        'api';
      }
    },
    planId: String,
    planStatus: String,
    clickType: String,
    versionEnable: Boolean,
  },
  created: function () {
    this.getMaintainerOptions();
    this.initTable();
    this.$EventBus.$on("API_TEST_END", this.handleTestEnd);
  },
  destroyed() {
    this.$EventBus.$off("API_TEST_END", this.handleTestEnd);
  },
  activated() {
    this.status = 'default';
  },
  watch: {
    selectNodeIds() {
      this.condition.selectAll = false;
      this.initTable();
    },
    currentProtocol() {
      this.initTable();
    },
    planId() {
      this.initTable();
    }
  },
  computed: {
    // 测试计划关联测试列表
    isRelevanceModel() {
      return this.model === 'relevance';
    },
    // 测试计划接口用例列表
    isPlanModel() {
      return this.model === 'plan';
    },
    // 接口定义用例列表
    isApiModel() {
      return this.model === 'api';
    },
    editTestPlanApiCaseOrder() {
      return editTestPlanApiCaseOrder;
    },
  },
  methods: {
    customHeader() {
      const list = deepClone(this.tableLabel);
      this.$refs.headerCustom.open(list);
    },
    getMaintainerOptions() {
      getProjectMemberUserFilter((data) => {
        this.userFilters = data;
      });
    },
    isApiListEnableChange(data) {
      this.$emit('isApiListEnableChange', data);
    },
    initTable() {
      if (this.$refs.table) {
        this.$refs.table.clear();
      }
      this.autoCheckStatus();
      this.condition.status = "";
      this.condition.moduleIds = this.selectNodeIds;
      if (this.currentProtocol != null) {
        this.condition.protocol = this.currentProtocol;
      }

      this.enableOrderDrag = (this.condition.orders && this.condition.orders.length) > 0 ? false : true;

      if (this.clickType) {
        if (this.status == 'default') {
          this.condition.status = this.clickType;
        } else {
          this.condition.status = null;
        }
        this.status = 'all';
      }
      if (this.planId) {
        this.condition.planId = this.planId;
        this.loading = true;
        testPlanApiCaseList({pageNum: this.currentPage, pageSize: this.pageSize}, this.condition)
          .then(response => {
            this.loading = false;
            this.total = response.data.itemCount;
            this.tableData = response.data.listObject;
            this.tableData.forEach(item => {
              if (item.tags && item.tags.length > 0) {
                item.tags = JSON.parse(item.tags);
              }
            });
          });
      }
    },
    search() {
      this.currentPage = 1;
      this.initTable();
    },
    buildPagePath(path) {
      return path + "/" + this.currentPage + "/" + this.pageSize;
    },
    reductionApi(row) {
      let ids = [row.id];
      apiTestCaseReduction(ids)
        .then(() => {
          this.$success(this.$t('commons.save_success'));
          this.search();
        });
    },
    handleDeleteBatch() {
      this.$alert(this.$t('test_track.plan_view.confirm_cancel_relevance') + "？", '', {
        confirmButtonText: this.$t('commons.confirm'),
        callback: (action) => {
          if (action === 'confirm') {
            let param = buildBatchParam(this);
            param.ids = this.$refs.table.selectIds;
            if (this.planId) {
              param.planId = this.planId;
              testPlanApiCaseBatchDelete(param)
                .then(() => {
                  if (this.$refs.table) {
                    this.$refs.table.clear();
                  }
                  this.initTable();
                  this.$emit('refresh');
                  this.$success(this.$t('test_track.cancel_relevance_success'));
                });
            }
          }
        }
      });
    },
    getResult(data) {
      if (RESULT_MAP.get(data)) {
        return RESULT_MAP.get(data);
      } else {
        return RESULT_MAP.get("default");
      }
    },
    runRefresh() {
      this.rowLoading = '';
      this.$success(this.$t('schedule.event_success'));
      this.autoCheckStatus();
      this.initTable();
    },
    singleRun(row) {
      let reportId = getUUID().substring(0, 8);
      this.rowLoading = row.id;
      run(row.id, reportId)
        .then(() => {
          this.runningReport.add(reportId);
          this.$refs.apiCaseResult.open(reportId);
        });
    },
    handleTestEnd(reportId) {
      if (this.runningReport.has(reportId)) {
        this.runRefresh();
        this.runningReport.delete(reportId);
      }
    },
    handleBatchEdit() {
      this.$refs.batchEdit.open(this.condition.selectAll ? this.total : this.$refs.table.selectRows.size);
      this.$refs.batchEdit.setSelectRows(this.$refs.table.selectRows);
    },
    getData() {
      return new Promise((resolve) => {
        let index = 1;
        this.runData = [];
        this.testPlanCaseIds = [];
        if (this.condition != null && this.condition.selectAll) {
          let selectAllRowParams = buildBatchParam(this);
          selectAllRowParams.ids = this.$refs.table.selectIds;
          testPlanApiCaseSelectAllTableRows(selectAllRowParams)
            .then(response => {
              let dataRows = response.data;
              // 按照列表顺序排序
              this.orderBySelectRows(dataRows);
              dataRows.forEach(row => {
                apiTestCaseGet(row.caseId)
                  .then((response) => {
                    let apiCase = response.data;
                    let request = JSON.parse(apiCase.request);
                    request.name = row.id;
                    request.id = row.id;
                    request.useEnvironment = row.environmentId;
                    this.runData.unshift(request);
                    this.testPlanCaseIds.unshift(row.id);
                    if (dataRows.length === index) {
                      resolve();
                    }
                    index++;
                  });
              });
            });
        } else {
          // 按照列表顺序排序
          let dataRows = this.orderBySelectRows(this.$refs.table.selectRows);
          dataRows.forEach(row => {
            apiTestCaseGet(row.caseId)
              .then((response) => {
                let apiCase = response.data;
                let request = JSON.parse(apiCase.request);
                request.name = row.id;
                request.id = row.id;
                request.useEnvironment = row.environmentId;
                this.runData.unshift(request);
                this.testPlanCaseIds.unshift(row.id);
                if (dataRows.length === index) {
                  resolve();
                }
                index++;
              });
          });
        }
      });
    },
    batchEdit(form) {
      let param = {};
      // 批量修改环境
      if (form.type === 'projectEnv') {
        let selectAllRowParams = buildBatchParam(this);
        selectAllRowParams.ids = this.$refs.table.selectIds;
        testPlanApiCaseSelectAllTableRows(selectAllRowParams)
          .then(response => {
            let dataRows = response.data;
            let map = new Map();
            param.projectEnvMap = strMapToObj(form.projectEnvMap);
            param.environmentType = form.environmentType;
            param.environmentGroupId = form.envGroupId;
            dataRows.forEach(row => {
              map[row.id] = row.projectId;
            });
            param.selectRows = map;
            testPlanApiCaseBatchUpdateEnv(param)
              .then(() => {
                this.$success(this.$t('commons.save_success'));
                this.initTable();
              });
          });
      } else {
        // 批量修改其它
      }
    },
    orderBySelectRows(rows) {
      let selectIds = Array.from(rows).map(row => row.id);
      let array = [];
      for (let i in this.tableData) {
        if (selectIds.indexOf(this.tableData[i].id) !== -1) {
          array.push(this.tableData[i]);
        }
      }
      return array;
    },
    handleBatchExecute() {
      this.getData().then(() => {
        if (this.runData && this.runData.length > 0) {
          this.$refs.runMode.open('API');
        }
      });
    },
    handleRunBatch(config) {
      let obj = {planIds: this.testPlanCaseIds, config: config, triggerMode: "BATCH"};
      testPlanApiCaseRun(obj)
        .then(() => {
          this.$message(this.$t('commons.run_message'));
          this.$refs.taskCenter.open();
          this.search();
        });
    },
    autoCheckStatus() { //  检查执行结果，自动更新计划状态
      if (!this.planId) {
        return;
      }
      testPlanAutoCheck(this.planId);
    },
    handleDelete(apiCase) {
      if (this.planId) {
        testPlanApiCaseDelete(apiCase.id)
          .then(() => {
            this.$success(this.$t('test_track.cancel_relevance_success'));
            this.$emit('refresh');
            this.initTable();
          });
      }
      return;
    },
    getProjectId() {
      if (!this.isRelevanceModel) {
        return getCurrentProjectID();
      } else {
        return this.currentCaseProjectId;
      }
    },
    getReportResult(apiCase) {
      this.$refs.apiCaseResult.openByCaseId(apiCase.id);
    },
    getVersionOptions() {
      if (hasLicense()) {
        getProjectVersions(getCurrentProjectID())
          .then(response => {
            this.versionOptions = response.data;
            this.versionFilters = response.data.map(u => {
              return {text: u.name, value: u.id};
            });
          });
      }
    },
    openApiById(item) {
      let definitionData = this.$router.resolve('/api/definition/' + getUUID() + "/apiTestCase/single:" + item.caseId + "/" + getCurrentProjectID() + "/" + item.protocol + "/" + getCurrentWorkspaceId());
      window.open(definitionData.href, '_blank');
    },
  },
};
</script>

<style scoped>
.operate-button > div {
  display: inline-block;
  margin-left: 10px;
}

.request-method {
  padding: 0 5px;
  color: #1E90FF;
}

.api-el-tag {
  color: white;
}

.search-input {
  float: right;
  width: 300px;
  /*margin-bottom: 20px;*/
  margin-right: 20px;
}

</style>
