package com.axelor.data.csv;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.axelor.data.ScriptHelper;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Objects.ToStringHelper;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

@XStreamAlias("bind")
public class CSVBinding {

	@XStreamAsAttribute
	private String column;

	@XStreamAlias("to")
	@XStreamAsAttribute
	private String field;
	
	@XStreamAsAttribute
	private String type;

	@XStreamAsAttribute
	private String search;
	
	@XStreamAsAttribute
	private boolean update;
	
	@XStreamAlias("eval")
	@XStreamAsAttribute
	private String expression;
	
	@XStreamAlias("if")
	@XStreamAsAttribute
	private String condition;
	
	@XStreamImplicit(itemFieldName = "bind")
	private List<CSVBinding> bindings;
	
	@XStreamAsAttribute
	private String adapter;

	public String getColumn() {
		return column;
	}
	
	public void setColumn(String column) {
		this.column = column;
	}

	public String getField() {
		return field;
	}
	
	public void setField(String field) {
		this.field = field;
	}
	
	public String getType() {
		return type;
	}

	public String getSearch() {
		return search;
	}
	
	public boolean isUpdate() {
		return update;
	}
	
	public String getExpression() {
		return expression;
	}
	
	public void setExpression(String expression) {
		this.expression = expression;
	}
	
	public String getCondition() {
		return condition;
	}
	
	public List<CSVBinding> getBindings() {
		return bindings;
	}
	
	public String getAdapter() {
		return adapter;
	}
	
	public static CSVBinding getBinding(final String column, final String field, Set<String> cols) {
		CSVBinding cb = new CSVBinding();
		cb.field = field;
		cb.column = column;
		
		if (cols == null || cols.isEmpty()) {
			if (cb.column == null)
				cb.column = field;
			return cb;
		}
		
		for(String col : cols) {
			if (cb.bindings == null)
				cb.bindings = Lists.newArrayList();
			cb.bindings.add(CSVBinding.getBinding(field + "." + col, col, null));
		}
		
		cb.update = true;
		cb.search = Joiner.on(" AND ").join(Collections2.transform(cols, new Function<String, String>(){
			
			@Override
			public String apply(String input) {
				return String.format("self.%s = :%s_%s_", input, field, input);
			}
		}));
		
		return cb;
	}
	
	private static ScriptHelper helper = new ScriptHelper(100, 10, false);
	
	public Object eval(Map<String, Object> context) {
		if (Strings.isNullOrEmpty(expression)) {
			return context.get(column);
		}
		return helper.eval(expression, context);
	}
	
	public boolean validate(Map<String, Object> context) {
		if (Strings.isNullOrEmpty(condition)) {
			return true;
		}
		String expr = condition + " ? true : false";
		return (Boolean) helper.eval(expr, context);
	}

	@Override
	public String toString() {
		
		ToStringHelper ts = Objects.toStringHelper(this);
		
		if (column != null) ts.add("column", column);
		if (field != null) ts.add("field", field);
		if (type != null) ts.add("type", type);
		if (bindings != null) ts.add("bindings", bindings).toString();
		
		return ts.toString();
	}
}
