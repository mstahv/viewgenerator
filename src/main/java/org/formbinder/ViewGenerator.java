/*
      Copyright 2012 Vaadin Ltd

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.formbinder;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.Form;
import com.vaadin.ui.FormFieldFactory;
import com.vaadin.ui.VerticalLayout;

/**
 * Utility to generate view stubs based on beans. Helps e.g. to get started with
 * FormBinder with large beans.
 */
public class ViewGenerator {

	private String[] args;
	private Class<?> beanClass;
	private FieldInfo[] fi;
	@SuppressWarnings("rawtypes")
	private BeanItem item;
	private FormFieldFactory ff;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ViewGenerator(String[] args) {
		addImport(CustomComponent.class);
		addImport(VerticalLayout.class);
		this.args = args;
		try {
			beanClass = getBean();

			Object newInstance = beanClass.newInstance();
			item = new BeanItem(newInstance);
			Collection<?> itemPropertyIds = item.getItemPropertyIds();
			fi = new FieldInfo[itemPropertyIds.size()];
			int i = 0;
			for (Object id : itemPropertyIds) {
				fi[i] = new FieldInfo(item.getItemProperty(id), (String) id);
				i++;
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public Item getItem() {
		return item;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) {
		try {
			ViewGenerator viewGenerator = new ViewGenerator(args);
			viewGenerator.generate(System.out);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("View generation failed!");
			System.out
					.println("Usage: java [CLASSPAHT] org.formbinder.ViewGenerator [OPTIONS] your.domain.Bean");
			System.out.println("Possible OPTIONSs:");
			System.out.println("-f=you.custom.fieldfactory.FiedlFactory");
		}
	}

	private void generate(PrintStream out) throws IOException {
		Velocity.setProperty("file.resource.loader.class",
				ClasspathResourceLoader.class.getName());
		Velocity.init();
		
		String templateName = "vd.vm";
		for (String a : args) {
			if (a.startsWith("-v7")) {
				templateName = "vd7.vm";
			}
		}
		
		Template template = Velocity.getTemplate(templateName);

		VelocityContext velocityContext = new VelocityContext();
		String packagenName = beanClass.getPackage().toString();
		packagenName = packagenName.replaceAll(".domain", ".ui");
		packagenName = packagenName.replaceAll(".model", ".ui");
		if (!(packagenName.contains(".ui.") || packagenName.endsWith(".ui"))) {
			packagenName = packagenName + ".ui";
		}
		velocityContext.put("package", packagenName);
		velocityContext.put("sn", beanClass.getSimpleName() + "View");
		velocityContext.put("pd", fi);
		velocityContext.put("imports", getImports());
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(out);
		template.merge(velocityContext, outputStreamWriter);
		outputStreamWriter.close();
	}

	private Class<?> getBean() throws ClassNotFoundException {
		String fqn = args[args.length - 1];
		Class<?> forName = Class.forName(fqn);
		return forName;
	}

	private HashSet<String> imports = new HashSet<String>();

	protected void addImport(Field field) {
		addImport(field.getClass());
	}
	@SuppressWarnings("rawtypes")
	protected void addImport(Class c) {
		imports.add(c.getName());
	}

	protected String getImports() {
		StringBuilder sb = new StringBuilder();
		for (String i : imports) {
			sb.append("import ");
			sb.append(i);
			sb.append(";\n");
		}
		return sb.toString();
	}

	public FormFieldFactory getFieldFactory() {
		if (ff == null) {
			if (args.length > 0) {
				for (String a : args) {
					if (a.startsWith("-f=")) {
						String fieldFactoryName = a.substring(3);
						try {
							Class<?> forName = Class.forName(fieldFactoryName);
							ff = (FormFieldFactory) forName.newInstance();
						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
			if (ff == null) {
				ff = DefaultFieldFactory.get();
			}
		}
		return ff;
	}

	private static final Component FAKECONTEX = new Form();

	public class FieldInfo {
		Property p;
		private Field field;
		private String name;

		public FieldInfo(Property itemProperty, String id) {
			this.p = itemProperty;
			this.name = id;
			field = getFieldFactory().createField(getItem(), getName(),
					FAKECONTEX);
			addImport(field);
		}

		public String getName() {
			return name;
		}

		public Field getField() {
			return field;
		}

		public String getFieldType() {
			return getField().getClass().getSimpleName();
		}

	}

}
