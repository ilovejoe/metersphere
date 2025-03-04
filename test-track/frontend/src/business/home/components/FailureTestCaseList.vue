<template>
  <el-card class="table-card" v-loading="loading" body-style="padding:10px;">
    <template v-slot:header>
      <span class="title">
        {{ $t('api_test.home_page.failed_case_list.title') }}
      </span>
    </template>
    <el-table border :data="tableData" class="adjust-table table-content" height="300px">
      <el-table-column prop="sortIndex" :label="$t('home.case.index')"
                       width="100" show-overflow-tooltip/>
      <el-table-column prop="caseName" :label="$t('home.case.case_name')"
                       width="150">
        <template v-slot:default="{row}">
          <el-link type="info" @click="redirect(row.caseType,row.id)">
            {{ row.caseName }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column
        prop="caseType"
        column-key="caseType"
        :label="$t('home.case.case_type')"
        width="150"
        show-overflow-tooltip>
        <template v-slot:default="scope">
          <ms-tag v-if="scope.row.caseType === 'apiCase'" type="success" effect="plain"
                  :content="$t('api_test.home_page.failed_case_list.table_value.case_type.api')"/>
          <ms-tag v-if="scope.row.caseType === 'scenario'" type="warning" effect="plain"
                  :content="$t('api_test.home_page.failed_case_list.table_value.case_type.scene')"/>
          <ms-tag v-if="scope.row.caseType === 'load'" type="danger" effect="plain"
                  :content="$t('api_test.home_page.failed_case_list.table_value.case_type.load')"/>
          <ms-tag v-if="scope.row.caseType === 'testCase'" effect="plain"
                  :content="$t('api_test.home_page.failed_case_list.table_value.case_type.functional')"/>
        </template>
      </el-table-column>
      <el-table-column prop="testPlan" :label="$t('home.case.test_plan')">
        <template v-slot:default="{row}">
          <div>
            <el-link type="info" @click="redirect('testPlanEdit',row.testPlanId)">
              {{ row.testPlan }}
            </el-link>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="failureTimes" :label="$t('home.case.failure_times')"
                       width="110" show-overflow-tooltip/>
    </el-table>
  </el-card>
</template>

<script>
import MsTag from "metersphere-frontend/src/components/MsTag";
import {getCurrentProjectID} from "metersphere-frontend/src/utils/token";
import {homeTestPlanFailureCaseGet} from "@/api/remote/api/api-home";

export default {
  name: "MsFailureTestCaseList",

  components: {
    MsTag
  },

  data() {
    return {
      tableData: [],
      loading: false
    }
  },
  props: {
    selectFunctionCase: Boolean,
  },
  computed: {
    projectId() {
      return getCurrentProjectID();
    },
  },
  methods: {
    search() {
      if (this.projectId) {
        this.loading = true;
        homeTestPlanFailureCaseGet(this.projectId, this.selectFunctionCase, 10)
          .then((r) => {
            this.loading = false;
            this.tableData = r.data;
          });
      }
    },
    redirect(pageType, param) {
      switch (pageType) {
        case "testPlanEdit":
          this.$emit('redirectPage', 'testPlanEdit', null, param);
          break;
        case "apiCase":
          this.$emit('redirectPage', 'api', 'apiTestCase', 'single:' + param);
          break;
        case "scenario":
          this.$emit('redirectPage', 'scenarioWithQuery', 'scenario', 'edit:' + param);
          break;
      }
    }
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
