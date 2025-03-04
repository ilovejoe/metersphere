<template>
  <div class="table-card" v-loading="result" body-style="padding:10px;">
    <el-table border :data="tableData" class="adjust-table table-content" height="260px">
      <el-table-column prop="num" :label="$t('home.new_case.index')" width="100"
                       show-overflow-tooltip>
        <template v-slot:default="{row}">
          <span type="num" @click="redirect(row.apiType,row.id)">
            {{ row.num }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="name" :label="$t('home.new_case.api_name')"
                       show-overflow-tooltip width="170">
        <template v-slot:default="{row}">
          <span type="name" @click="redirect(row.apiType,row.id)">
            {{ row.name }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="path" :label="$t('home.new_case.path')" width="170"
                       show-overflow-tooltip>

      </el-table-column>
      <el-table-column prop="status" :label="$t('home.new_case.api_status')">
        <template v-slot:default="scope">
          <span class="el-dropdown-link">
            <api-status :value="scope.row.status"/>
          </span>
        </template>
      </el-table-column>
      <el-table-column :label="$t('home.new_case.update_time')" width="170">
        <template v-slot:default="scope">
          <span>{{ scope.row.updateTime | datetimeFormat }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="caseTotal" :label="$t('home.new_case.relation_case')"
                       width="100">
        <template v-slot:default="{row}">
          <el-link type="info" @click="redirect(row.caseType,row.id)">
            {{ row.caseTotal }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="scenarioTotal"
                       :label="$t('home.new_case.relation_scenario')"
                       width="100">
        <template v-slot:default="{row}">
          <el-link type="info" @click="redirect(row.scenarioType,row.ids)">
            {{ row.scenarioTotal }}
          </el-link>
        </template>
      </el-table-column>

    </el-table>
    <ms-table-pagination :change="search" :current-page.sync="currentPage" :page-size.sync="pageSize"
                         :total="total"/>
  </div>
</template>

<script>
import {definitionWeekList} from "@/api/definition";
import MsTag from "metersphere-frontend/src/components/MsTag";
import {getCurrentProjectID} from "metersphere-frontend/src/utils/token";
import {getUUID} from "metersphere-frontend/src/utils";
import {API_DEFINITION_CONFIGS} from "metersphere-frontend/src/components/search/search-components";
import {API_STATUS} from "@/business/definition/model/JsonData";
import ApiStatus from "@/business/definition/components/list/ApiStatus";
import MsTablePagination from "metersphere-frontend/src/components/pagination/TablePagination";
import {initCondition} from "metersphere-frontend/src/utils/tableUtils";

export default {
  name: "MsApiNewTestCaseList",

  components: {
    MsTag, ApiStatus, MsTablePagination
  },

  data() {
    return {
      result: false,
      tableData: [],
      loading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0,
      condition: {
        components: API_DEFINITION_CONFIGS,
      },
      status: API_STATUS,
      currentProtocol: "HTTP",
    }
  },
  computed: {
    projectId() {
      return getCurrentProjectID();
    },
  },
  methods: {
    search(currentProtocol) {
      if (this.$refs.table) {
        this.$refs.table.clear();
      }
      initCondition(this.condition, this.condition.selectAll);
      this.selectDataCounts = 0;
      if (!this.trashEnable) {
        this.condition.moduleIds = this.selectNodeIds;
      }
      this.condition.projectId = this.projectId;

      this.enableOrderDrag = (this.condition.orders && this.condition.orders.length) > 0 ? false : true;

      //检查是否只查询本周数据
      this.getSelectDataRange();
      this.condition.selectThisWeedData = false;
      this.condition.apiCaseCoverage = null;
      switch (this.selectDataRange) {
        case 'thisWeekCount':
          this.condition.selectThisWeedData = true;
          break;
        case 'uncoverage':
          this.condition.apiCaseCoverage = 'uncoverage';
          break;
        case 'coverage':
          this.condition.apiCaseCoverage = 'coverage';
          break;
        case 'Prepare':
          this.condition.filters.status = [this.selectDataRange];
          break;
        case 'Completed':
          this.condition.filters.status = [this.selectDataRange];
          break;
        case 'Underway':
          this.condition.filters.status = [this.selectDataRange];
          break;
      }
      if (currentProtocol) {
        this.condition.moduleIds = [];
      }
      if (this.condition.projectId) {
        this.result = definitionWeekList(this.currentPage, this.pageSize, this.condition).then(response => {
          this.total = response.data.itemCount;
          this.tableData = response.data.listObject;
        });
      }
    },
    //判断是否只显示本周的数据。  从首页跳转过来的请求会带有相关参数
    getSelectDataRange() {
      let dataRange = this.$route.params.dataSelectRange;
      let dataType = this.$route.params.dataType;
      if (dataType === 'api') {
        this.selectDataRange = dataRange;
      } else {
        this.selectDataRange = 'all';
      }
    },
    redirect(pageType, param) {
      //api页面跳转
      //传入UUID是为了进行页面重新加载判断
      let resolve;
      let uuid = getUUID();
      switch (pageType) {
        case "api":
          resolve = this.$router.resolve({
            name: 'ApiDefinitionWithQuery',
            params: {
              redirectID: getUUID(),
              dataType: "api",
              dataSelectRange: 'edit:' + param,
            }
          });
          window.open(resolve.href, '_blank');
          break;
        case "apiCase":
          this.$emit('redirectPage', 'api', 'apiTestCase', 'singleList:' + param);
          break;
        case "scenario":
          if (param) {
            this.$emit('redirectPage', 'scenario', 'scenario', 'list:' + param);
            break;
          }
      }
    },
  },
  created() {
    this.search();
  },
  activated() {
    this.search();
  }
}
</script>

<style scoped>

.el-table {
  cursor: pointer;
}

.el-card :deep(.el-card__header) {
  border-bottom: 0px solid #EBEEF5;
}

</style>
