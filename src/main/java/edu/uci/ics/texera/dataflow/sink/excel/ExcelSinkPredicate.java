package edu.uci.ics.texera.dataflow.sink.excel;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

import edu.uci.ics.texera.dataflow.common.OperatorGroupConstants;
import edu.uci.ics.texera.dataflow.common.PredicateBase;
import edu.uci.ics.texera.dataflow.common.PropertyNameConstants;

public class ExcelSinkPredicate extends PredicateBase {
    
    private Integer limit;
    private Integer offset;
    
    public ExcelSinkPredicate() {
        this.limit = Integer.MAX_VALUE;
        this.offset = 0;
    };
    
    @JsonCreator
    public ExcelSinkPredicate(
            @JsonProperty(value = PropertyNameConstants.LIMIT, required = false)
            Integer limit,
            @JsonProperty(value = PropertyNameConstants.OFFSET, required = false)
            Integer offset
            ) {
        this.limit = limit;
        if (this.limit == null) {
            this.limit = Integer.MAX_VALUE;
        }
        this.offset = offset;
        if (this.offset == null) {
            this.offset = 0;
        }
    }
    
    @JsonProperty(value = PropertyNameConstants.LIMIT)
    public Integer getLimit() {
        return this.limit;
    }
    
    @JsonProperty(value = PropertyNameConstants.OFFSET)
    public Integer getOffset() {
        return this.offset;
    }
    
    @Override
    public ExcelSink newOperator() {
        return new ExcelSink(this);
    }
    
    public static Map<String, Object> getOperatorMetadata() {
        return ImmutableMap.<String, Object>builder()
            .put(PropertyNameConstants.USER_FRIENDLY_NAME, "Write to Excel file")
            .put(PropertyNameConstants.OPERATOR_DESCRIPTION, "Write the results to an excel file")
            .put(PropertyNameConstants.OPERATOR_GROUP_NAME, OperatorGroupConstants.DATABASE_GROUP)
            .build();
    }

}
