<template>
  <ms-container>
    <ms-main-container class="report-content">
      <el-card v-loading="loading">
        <test-plan-report-buttons :is-db="isDb" :plan-id="planId" :is-share="isShare" :report="report"
                                  v-if="!isTemplate && !isShare"/>
        <test-plan-overview-report v-if="overviewEnable" :report="report"/>
        <test-plan-summary-report v-if="summaryEnable" :is-db="isDb" :is-template="isTemplate" :is-share="isShare"
                                  :report="report" :plan-id="planId"/>
        <test-plan-functional-report v-if="functionalEnable" :is-db="isDb" :share-id="shareId" :is-share="isShare"
                                     :is-template="isTemplate" :plan-id="planId" :report="report"/>
        <test-plan-api-report v-if="apiEnable" :is-db="isDb" :share-id="shareId" :is-share="isShare"
                              :is-template="isTemplate" :report="report" :plan-id="planId"/>
        <test-plan-ui-report v-if="uiEnable" :is-db="isDb" :share-id="shareId" :is-share="isShare"
                             :is-template="isTemplate" :report="report" :plan-id="planId"/>
        <test-plan-load-report v-if="loadEnable" :is-db="isDb" :share-id="shareId" :is-share="isShare"
                               :is-template="isTemplate" :report="report" :plan-id="planId"/>
      </el-card>
    </ms-main-container>
    <test-plan-report-navigation-bar
      :api-enable="apiEnable"
      :need-move-bar="needMoveBar"
      :summary-enable="summaryEnable"
      :functional-enable="functionalEnable"
      :load-enable="loadEnable"
      :ui-enable="uiEnable"
      :overview-enable="overviewEnable"
      :is-template="isTemplate"/>
  </ms-container>
</template>

<script>
import TestPlanFunctionalReport from "@/business/plan/view/comonents/report/detail/TestPlanFunctionalReport";
import {
  getShareTestPlanReport,
  getShareTestPlanReportContent,
  getTestPlanReport,
  getTestPlanReportContent
} from "@/api/remote/plan/test-plan";
import TestPlanApiReport from "@/business/plan/view/comonents/report/detail/TestPlanApiReport";
import TestPlanUiReport from "@/business/plan/view/comonents/report/detail/TestPlanUiReport";
import TestPlanLoadReport from "@/business/plan/view/comonents/report/detail/TestPlanLoadReport";
import TestPlanReportContainer from "@/business/plan/view/comonents/report/detail/TestPlanReportContainer";
import TestPlanOverviewReport from "@/business/plan/view/comonents/report/detail/TestPlanOverviewReport";
import TestPlanSummaryReport from "@/business/plan/view/comonents/report/detail/TestPlanSummaryReport";
import TestPlanReportButtons from "@/business/plan/view/comonents/report/detail/TestPlanReportButtons";
import TestPlanReportNavigationBar from "@/business/plan/view/comonents/report/detail/TestPlanReportNavigationBar";
import MsContainer from "metersphere-frontend/src/components/MsContainer";
import MsMainContainer from "metersphere-frontend/src/components/MsMainContainer";

export default {
  name: "TestPlanReportContent",
  components: {
    MsMainContainer,
    MsContainer,
    TestPlanReportNavigationBar,
    TestPlanReportButtons,
    TestPlanSummaryReport,
    TestPlanOverviewReport,
    TestPlanReportContainer,
    TestPlanLoadReport,
    TestPlanApiReport,
    TestPlanFunctionalReport,
    TestPlanUiReport,
  },
  props: {
    planId: String,
    isShare: Boolean,
    isTemplate: Boolean,
    isDb: Boolean,
    shareId: String,
    reportId: String,
    needMoveBar: Boolean
  },
  data() {
    return {
      report: {},
      loading: false,
      shareUrl: ''
    };
  },
  watch: {
    planId() {
      this.getReport();
    },
    reportId() {
      this.getReport();
    },
    planReportTemplate() {
      if (this.planReportTemplate) {
        this.init();
      }
    },
  },
  created() {
    this.getReport();
  },
  computed: {
    overviewEnable() {
      let disable = this.report.config
        && this.report.config.overview && this.report.config.overview.enable === false;
      return !disable;
    },
    summaryEnable() {
      let disable = this.report.config && this.report.config.summary
        && this.report.config.summary.enable === false;
      return !disable;
    },
    functionalEnable() {
      let disable = this.report.config && this.report.config.functional.enable === false;
      return !disable && this.report.functionResult
        && this.report.functionResult.caseData && this.report.functionResult.caseData.length > 0;
    },
    apiEnable() {
      let disable = this.report.config && this.report.config.api.enable === false;
      return !disable && ((this.report.apiResult &&
        (
          (this.report.apiResult.apiCaseData && this.report.apiResult.apiCaseData.length > 0)
          || (this.report.apiResult.apiScenarioData && this.report.apiResult.apiScenarioData.length > 0)
        )) || (this.report.apiAllCases && this.report.apiAllCases.length > 0) || (this.report.scenarioAllCases && this.report.scenarioAllCases.length > 0));

    },
    loadEnable() {
      let disable = this.report.config && this.report.config.load.enable === false;
      return !disable && this.report.loadResult && this.report.loadResult.caseData && this.report.loadResult.caseData.length > 0
        || (this.report.loadAllCases && this.report.loadAllCases.length > 0);
    },
    uiEnable() {
      let disable = this.report.config && this.report.config.ui.enable === false;
      return !disable && this.report.uiResult
        && this.report.uiResult.uiScenarioStepData && this.report.uiResult.uiScenarioStepData.length > 0
        ||  (this.report.uiAllCases && this.report.uiAllCases.length > 0);
    }
  },
  methods: {
    getReport() {
      if (this.isTemplate) {
        this.report = "#report";
        if (this.report.lang) {
          this.$setLang(this.report.lang);
        }
        this.report.config = this.getDefaultConfig(this.report);
      } else if (this.isDb) {
        if (this.isShare) {
          //持久化的报告分享
          this.loading = true;
          getShareTestPlanReportContent(this.shareId, this.reportId)
            .then((r) => {
              this.loading = false;
              this.report = r.data;
              this.report.config = this.getDefaultConfig(this.report);
            });
        } else {
          this.loading = true;
          getTestPlanReportContent(this.reportId)
            .then((r) => {
              this.loading = false;
              this.report = r.data;
              this.report.config = this.getDefaultConfig(this.report);
            });
        }
      } else if (this.isShare) {
        this.loading = true;
        getShareTestPlanReport(this.shareId, this.planId)
          .then((r) => {
            this.loading = false;
            this.report = r.data;
            this.report.config = this.getDefaultConfig(this.report);
          });
      } else {
        this.loading = true;
        getTestPlanReport(this.planId)
          .then((r) => {
            this.loading = false;
            this.report = r.data;
            this.report.config = this.getDefaultConfig(this.report);
          });
      }
    },
    getDefaultConfig(report) {
      let dbConfig = null;
      if (report && report.config) {
        let configStr = report.config;
        if (configStr) {
          dbConfig = JSON.parse(configStr);
        }
      }
      let config = {
        overview: {
          enable: true,
          name: this.$t('test_track.report.overview')
        },
        summary: {
          enable: true,
          name: this.$t('test_track.report.report_summary')
        },
        functional: {
          enable: true,
          name: this.$t('test_track.report.analysis_functional'),
          children: {
            result: {
              enable: true,
              name: this.$t('test_track.report.test_result'),
            },
            issue: {
              enable: true,
              name: this.$t('test_track.report.issue_list'),
            },
            all: {
              enable: true,
              name: this.$t('test_track.report.all_case'),
            },
            failure: {
              enable: true,
              name: this.$t('test_track.report.fail_case'),
            },
            blocking: {
              enable: true,
              name: this.$t('test_track.plan_view.blocking') + this.$t('commons.track'),
            },
            skip: {
              enable: true,
              name: this.$t('test_track.plan_view.skip') + this.$t('commons.track'),
            },
          }
        },
        api: {
          enable: true,
          name: this.$t('test_track.report.analysis_api'),
          children: {
            result: {
              enable: true,
              name: this.$t('test_track.report.test_result'),
            },
            failure: {
              enable: true,
              name: this.$t('test_track.report.fail_case'),
            },
            errorReport: {
              enable: true,
              name: this.$t('error_report_library.option.name'),
            },
            unExecute: {
              enable: true,
              name: this.$t('api_test.home_page.detail_card.unexecute'),
            },
            all: {
              enable: true,
              name: this.$t('test_track.report.all_case'),
            }
          }
        },
        load: {
          enable: true,
          name: this.$t('test_track.report.analysis_load'),
          children: {
            result: {
              enable: true,
              name: this.$t('test_track.report.test_result'),
            },
            failure: {
              enable: true,
              name: this.$t('test_track.report.fail_case'),
            },
            all: {
              enable: true,
              name: this.$t('test_track.report.all_case'),
            }
          }
        },
        ui: {
          enable: true,
          name: this.$t('test_track.report.analysis_ui'),
          children: {
            result: {
              enable: true,
              name: this.$t('test_track.report.test_result'),
            },
            failure: {
              enable: true,
              name: this.$t('test_track.report.fail_case'),
            },
            unExecute: {
              enable: true,
              name: this.$t('api_test.home_page.detail_card.unexecute'),
            },
            all: {
              enable: true,
              name: this.$t('test_track.report.all_case'),
            }
          }
        },
      };
      if (dbConfig) {
        this.mergeConfig(config, dbConfig);
      }
      return config;
    },
    mergeConfig(config, dbConfig) {
      for (let key of Object.keys(config)) {
        if (dbConfig[key]) {
          config[key].enable = dbConfig[key].enable;
          if (config[key].children && dbConfig[key].children) {
            this.mergeConfig(config[key].children, dbConfig[key].children);
          }
        }
      }
    }
  }
}
</script>

<style scoped>

.el-card {
  /*width: 95% !important;*/
  padding: 15px;
}

:deep(.el-tabs .el-tabs__header) {
  padding-left: 15px;
  padding-right: 15px;
  padding-top: 15px;
}

:deep(.empty) {
  text-align: center;
  width: 100%;
  padding: 40px;
}

:deep(.padding-col) {
  padding: 5px;
}

:deep(.el-scrollbar) {
  height: 100%;
}

:deep(.el-card .ms-table) {
  cursor: pointer;
}
</style>
