/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2012-2014 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.meta.schema.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.joda.time.DateTime;

import com.axelor.app.AppSettings;
import com.axelor.common.ClassUtils;
import com.axelor.common.FileUtils;
import com.axelor.db.JPA;
import com.axelor.meta.ActionHandler;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

@XmlType
public class ActionExport extends Action {

	public static final String EXPORT_PATH = AppSettings.get().getPath("data.export.dir", "{java.io.tmpdir}/axelor/data-export");
	private static final String DEFAULT_DIR = "${date}/${name}";

	@XmlAttribute(name = "output")
	private String output;
	
	@XmlAttribute(name = "download")
	private Boolean download;
	
	@XmlElement(name = "export")
	private List<Export> exports;

	public String getOutput() {
		return output;
	}
	
	public Boolean getDownload() {
		return download;
	}

	public List<Export> getExports() {
		return exports;
	}
	
	protected String doExport(String dir, Export export, ActionHandler handler) throws IOException {
		export.template = handler.evaluate(export.template).toString();

		Reader reader = null;
		File template = new File(export.template);
		if (template.isFile()) {
			reader = new FileReader(template);
		}
		
		if (reader == null) {
			InputStream is = ClassUtils.getResourceStream(export.template);
			if (is == null) {
				throw new FileNotFoundException("No such template: " + export.template);
			}
			reader = new InputStreamReader(is);
		}

		String name = export.getName();
		if (name.indexOf("$") > -1) {
			name = handler.evaluate("eval: \"\"\"" + name + "\"\"\"").toString();
		}

		log.info("export {} as {}", export.getTemplate(), name);

		File output = FileUtils.getFile(EXPORT_PATH, dir, name);
		String contents = null;
		try {
			contents = handler.template(reader);
		} finally {
			reader.close();
		}
		
		Files.createParentDirs(output);
		Files.write(contents, output, Charsets.UTF_8);
		
		log.info("file saved: {}", output);
		
		return FileUtils.getFile(dir, name).toString();
	}

	@Override
	public Object evaluate(ActionHandler handler) {
		log.info("action-export: {}", getName());

		String dir = output == null ? DEFAULT_DIR : output;

		dir = dir.replace("${name}", getName())
				 .replace("${date}", new DateTime().toString("yyyyMMdd"))
				 .replace("${time}", new DateTime().toString("HHmmss"));
		dir = handler.evaluate(dir).toString();
		
		for(Export export : exports) {
			if(!export.test(handler)){
				continue;
			}
			try {
				String file = doExport(dir, export, handler);
				if (getDownload() == Boolean.TRUE) {
					return ImmutableMap.of("exportFile", file, "notify", JPA.translate("Export complete."));
				}
				return ImmutableMap.of("notify", JPA.translate("Export complete."));
			} catch (Exception e) {
				log.error("error while exporting: ", e);
				return ImmutableMap.of("error", e.getMessage());
			}
		}
		return null;
	}

	public Object wrap(ActionHandler handler) {
		return evaluate(handler);
	}

	@XmlType
	public static class Export extends Element {
		
		@XmlAttribute
		private String template;
		
		public String getTemplate() {
			return template;
		}
		
		@Override
		public String toString() {
			return Objects.toStringHelper(getClass())
					.add("name", getName())
					.add("template", template)
					.toString();
		}
	}
}
