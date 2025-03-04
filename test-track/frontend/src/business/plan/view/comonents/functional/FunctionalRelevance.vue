<template>

  <test-case-relevance-base
    @setProject="setProject"
    @save="saveCaseRelevance"
    :flag="isTestPlan"
    :multiple-project="multipleProject"
    :is-saving="isSaving"
    ref="baseRelevance">

    <template v-slot:aside>
      <node-tree class="node-tree"
                 v-loading="nodeResult.loading"
                 local-suffix="test_case"
                 default-label="未规划用例"
                 @nodeSelectEvent="nodeChange"
                 :tree-nodes="treeNodes"
                 ref="nodeTree"/>
    </template>

    <ms-table-header :condition.sync="page.condition" @search="search" title="" :show-create="false">
      <template v-slot:searchBarBefore>
        <version-select v-xpack :project-id="projectId" @changeVersion="changeVersion" margin-right="20"/>
      </template>
    </ms-table-header>

    <ms-table
      v-loading="page.result.loading"
      :data="page.data"
      :condition="page.condition"
      :total="page.total"
      :page-size.sync="page.pageSize"
      :screen-height="screenHeight"
      @handlePageChange="getTestCases"
      @selectCountChange="setSelectCounts"
      @order="getTestCases"
      @filter="search"
      ref="table">

      <ms-table-column
        v-if="!customNum"
        prop="num"
        sortable
        :label="$t('commons.id')">
      </ms-table-column>
      <ms-table-column
        v-if="customNum"
        prop="customNum"
        sortable
        :label="$t('commons.id')">
      </ms-table-column>

      <ms-table-column prop="name" :label="$t('commons.name')"/>

      <ms-table-column
        v-if="versionEnable"
        prop="versionId"
        :filters="versionFilters"
        :label="$t('commons.version')"
        show-overflow-tooltip>
        <template v-slot:default="scope">
          <span>{{ scope.row.versionName }}</span>
        </template>
      </ms-table-column>

      <ms-table-column
        prop="priority"
        :filters="priorityFilters"
        sortable
        :label="$t('test_track.case.priority')"
        width="120px">
        <template v-slot:default="scope">
          <priority-table-item :value="scope.row.priority"/>
        </template>
      </ms-table-column>

      <test-plan-case-status-table-item
        sortable
        prop="lastExecuteResult"/>

      <test-case-review-status-table-item sortable/>

      <ms-table-column prop="tags" :label="$t('commons.tag')" width="90px">
        <template v-slot:default="scope">
          <ms-tag v-for="(itemName, index)  in scope.row.tags" :key="index" type="success" effect="plain"
                  :content="itemName" style="margin-left: 0px; margin-right: 2px"/>
          <span/>
        </template>
      </ms-table-column>

      <ms-update-time-column/>
      <ms-create-time-column/>

    </ms-table>

    <ms-table-pagination :change="getTestCases" :current-page.sync="page.currentPage" :page-size.sync="page.pageSize"
                         :total="page.total"/>
  </test-case-relevance-base>

</template>

<script>

import NodeTree from 'metersphere-frontend/src/components/module/MsNodeTree';
import PriorityTableItem from "../../../../common/tableItems/planview/PriorityTableItem";
import TypeTableItem from "../../../../common/tableItems/planview/TypeTableItem";
import MsTableSearchBar from "metersphere-frontend/src/components/MsTableSearchBar";
import MsTableAdvSearchBar from "metersphere-frontend/src/components/search/MsTableAdvSearchBar";
import MsTableHeader from "metersphere-frontend/src/components/MsTableHeader";
import TestCaseRelevanceBase from "../base/TestCaseRelevanceBase";
import MsTableColumn from "metersphere-frontend/src/components/table/MsTableColumn";
import MsTable from "metersphere-frontend/src/components/table/MsTable";
import MsTablePagination from "metersphere-frontend/src/components/pagination/TablePagination";
import MsTag from "metersphere-frontend/src/components/MsTag";
import MsCreateTimeColumn from "metersphere-frontend/src/components/table/MsCreateTimeColumn";
import MsUpdateTimeColumn from "metersphere-frontend/src/components/table/MsUpdateTimeColumn";
import StatusTableItem from "@/business/common/tableItems/planview/StatusTableItem";
import ReviewStatus from "@/business/case/components/ReviewStatus";
import TestCaseReviewStatusTableItem from "@/business/common/tableItems/TestCaseReviewStatusTableItem";
import TestPlanCaseStatusTableItem from "@/business/common/tableItems/TestPlanCaseStatusTableItem";
import {TEST_CASE_CONFIGS} from "metersphere-frontend/src/components/search/search-components";
import MxVersionSelect from "metersphere-frontend/src/components/version/MxVersionSelect";
import {getProjectApplicationConfig} from "@/api/project-application";
import {getVersionFilters} from "@/business/utils/sdk-utils";

export default {
  name: "FunctionalRelevance",
  components: {
    TestPlanCaseStatusTableItem,
    TestCaseReviewStatusTableItem,
    ReviewStatus,
    StatusTableItem,
    MsUpdateTimeColumn,
    MsCreateTimeColumn,
    MsTag,
    MsTablePagination,
    MsTable,
    MsTableColumn,
    TestCaseRelevanceBase,
    NodeTree,
    PriorityTableItem,
    TypeTableItem,
    MsTableSearchBar,
    MsTableAdvSearchBar,
    MsTableHeader,
    'VersionSelect': MxVersionSelect,
  },
  mounted() {
    this.getVersionOptions();
  },
  data() {
    return {
      nodeResult: {},
      isSaving: false,
      loading: false,
      treeNodes: [],
      selectNodeIds: [],
      selectNodeNames: [],
      projectId: '',
      projectName: '',
      projects: [],
      customNum: false,
      screenHeight: '400',
      priorityFilters: [
        {text: 'P0', value: 'P0'},
        {text: 'P1', value: 'P1'},
        {text: 'P2', value: 'P2'},
        {text: 'P3', value: 'P3'}
      ],
      versionFilters: []
    };
  },
  props: {
    page: {
      type: Object
    },
    isTestPlan: {
      type: Boolean
    },
    getTableData: {
      type: Function
    },
    getNodeTree: {
      type: Function
    },
    save: {
      type: Function
    },
    multipleProject: {
      type: Boolean,
      default: true
    },
    versionEnable: {
      type: Boolean,
      default: false
    },
  },
  watch: {
    selectNodeIds() {
      this.getTestCases();
    },
    projectId() {
      this.setConditionModuleIdParam();
      this.page.condition.projectId = this.projectId;
      this.page.condition.versionId = null;
      this.getProjectNode();
      this.getTestCases();
      this.getCustomNum();
      this.getVersionOptions();
    }
  },
  methods: {
    open() {
      this.page.condition = {components: TEST_CASE_CONFIGS};
      this.isSaving = false;
      this.$refs.baseRelevance.open();
      if (this.$refs.table) {
        this.$refs.table.clear();
      }
      if (this.projectId) {
        this.getProjectNode(this.projectId);
      }
    },
    setProject(projectId) {
      this.projectId = projectId;
    },
    setConditionModuleIdParam() {
      this.page.condition.components.forEach(component => {
        if (component.key === 'moduleIds') {
          component.options.params = {"projectId": this.projectId};
        }
      });
    },
    getCustomNum() {
      getProjectApplicationConfig('CASE_CUSTOM_NUM')
        .then(result => {
          let data = result.data;
          if (data) {
            this.customNum = data.caseCustomNum;
          }
        });
    },
    search() {
      // 添加搜索条件时，当前页设置成第一页
      this.page.currentPage = 1;
      this.getTestCases();
      this.getProjectNode(this.projectId, this.page.condition);
    },
    getTestCases() {
      let condition = this.page.condition;
      if (this.selectNodeIds && this.selectNodeIds.length > 0) {
        condition.nodeIds = this.selectNodeIds;
      } else {
        condition.nodeIds = [];
      }
      condition.projectId = this.projectId;
      if (this.projectId) {
        this.getTableData();
      }
    },
    saveCaseRelevance(item) {
      this.isSaving = true;
      let param = {};
      param.ids = this.$refs.table.selectIds;
      param.request = this.page.condition;
      param.checked = item;
      this.save(param, this);
    },
    nodeChange(node, nodeIds, nodeNames) {
      this.page.condition.selectAll = false;
      this.$refs.table.condition.selectAll = false;
      this.selectNodeIds = nodeIds;
      this.selectNodeNames = nodeNames;
    },
    close() {
      this.selectNodeIds = [];
      this.selectNodeNames = [];
      this.$refs.table.clear();
    },
    getProjectNode(projectId, condition) {
      const index = this.projects.findIndex(project => project.id === projectId);
      if (index !== -1) {
        this.projectName = this.projects[index].name;
      }
      if (projectId) {
        this.projectId = projectId;
      }
      this.getNodeTree(this, condition);
    },
    getVersionOptions() {
      getVersionFilters(this.projectId)
        .then(r => this.versionFilters = r.data);
    },
    changeVersion(currentVersion) {
      this.page.condition.versionId = currentVersion || null;
      this.getTestCases();
      this.getProjectNode(this.projectId, this.page.condition);
    },
    setSelectCounts(data) {
      this.$refs.baseRelevance.selectCounts = data;
    }
  }
};
</script>

<style scoped>
</style>
