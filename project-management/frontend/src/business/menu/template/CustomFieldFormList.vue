<template>
  <ms-table
    v-loading="loading"
    :enable-selection="false"
    :operators="operators"
    :data="tableData"
    :screen-height="null"
    @refresh="refreshTable"
    ref="table">

    <ms-table-column
      :label="$t('commons.name')"
      prop="name">
      <template v-slot="scope">
        <span v-if="scope.row.system">
          {{ $t(systemNameMap[scope.row.name]) }}
        </span>
        <span v-else>
          {{ scope.row.name }}
        </span>
      </template>
    </ms-table-column>

    <ms-table-column
      :label="$t('commons.default')"
      min-width="200"
      prop="type">
      <template v-slot="scope">
        <el-scrollbar>
          <custom-filed-component
            class="default-value-item"
            :data="scope.row"
            prop="defaultValue"
            :is-template-edit="true"/>
        </el-scrollbar>
      </template>
    </ms-table-column>

    <field-custom-data-table-item :scene="scene"/>

    <ms-table-column
      :label="$t('api_test.definition.document.table_coloum.is_required')"
      width="80"
      prop="type">
      <template v-slot="scope">
        <el-checkbox v-model="scope.row.required"/>
      </template>
    </ms-table-column>

    <ms-table-column
      :label="$t('custom_field.system_field')"
      width="80"
      prop="system">
      <template v-slot="scope">
        <span v-if="scope.row.system">
          {{ $t('commons.yes') }}
        </span>
        <span v-else>
          {{ $t('commons.no') }}
        </span>
      </template>
    </ms-table-column>

    <ms-table-column
      :label="$t('commons.remark')"
      prop="remark">
    </ms-table-column>

  </ms-table>

</template>

<script>
import MsTableOperatorButton from "metersphere-frontend/src/components/MsTableOperatorButton";
import MsTable from "metersphere-frontend/src/components/table/MsTable";
import MsTableColumn from "metersphere-frontend/src/components/table/MsTableColumn";
import FieldCustomDataTableItem from "./FieldCustomDataTableItem";
import CustomFiledComponent from "metersphere-frontend/src/components/template/CustomFiledComponent";
import {SYSTEM_FIELD_NAME_MAP} from "metersphere-frontend/src/utils/table-constants";
import {getCustomFields} from "../../../api/template";

export default {
  name: "CustomFieldFormList",
  components: {
    CustomFiledComponent,
    FieldCustomDataTableItem,
    MsTableColumn,
    MsTable,
    MsTableOperatorButton
  },
  data() {
    return {
      result: {},
      loading: false,
      operators: [
        {
          tip: this.$t('commons.delete'), icon: "el-icon-delete", type: "danger",
          exec: this.handleDelete,
          isDisable: row => row.name === '用例等级'
        }
      ],
    };
  },
  props: {
    tableData: {
      type: Array,
      default() {
        return [];
      },
    },
    scene: String,
    platform: String,
    templateContainIds: Set
  },
  watch: {
    'customFieldIds.length'() {
      this.initTableData();
    }
  },
  computed: {
    systemNameMap() {
      return SYSTEM_FIELD_NAME_MAP;
    }
  },
  methods: {
    handleDelete(item, index) {
      this.templateContainIds.delete(item.fieldId);
      this.tableData.splice(index, 1);
    },
    refreshTable() {
      this.$refs.table.reloadTable();
    },
    appendData(customFieldIds) {
      let condition = {};
      condition.ids = customFieldIds;
      this.loading = getCustomFields(condition).then((response) => {
        let data = response.data;
        data.forEach(item => {
          if (item.id) {
            this.templateContainIds.add(item.id);
          }
          item.fieldId = item.id;
          item.id = null;
          item.options = JSON.parse(item.options);
          if (item.type === 'checkbox') {
            item.defaultValue = [];
          }
        });
        this.tableData.push(...data);
      });
    }
  }
};
</script>

<style scoped>

:deep(.el-table--border, .el-table--group ) {
  border: 0;
}
</style>
