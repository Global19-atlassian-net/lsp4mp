/*******************************************************************************
* Copyright (c) 2020 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.lsp4mp.jdt.core.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.lsp4mp.jdt.internal.core.project.IConfigSource;
import org.eclipse.lsp4mp.jdt.internal.core.project.PropertiesConfigSource;
import org.eclipse.lsp4mp.jdt.internal.core.project.YamlConfigSource;

/**
 * JDT MicroProfile project.
 *
 * @author Angelo ZERR
 *
 */
public class JDTMicroProfileProject {

	public static final String MICROPROFILE_CONFIG_PROPERTIES_FILE = "META-INF/microprofile-config.properties";

	@Deprecated
	public static final String APPLICATION_PROPERTIES_FILE = "application.properties";
	@Deprecated
	public static final String APPLICATION_YAML_FILE = "application.yaml";

	private final List<IConfigSource> configSources;

	public JDTMicroProfileProject(IJavaProject javaProject) {
		this.configSources = new ArrayList<IConfigSource>(3);
		configSources.add(new YamlConfigSource(APPLICATION_YAML_FILE, javaProject));
		configSources.add(new PropertiesConfigSource(APPLICATION_PROPERTIES_FILE, javaProject));
		configSources.add(new PropertiesConfigSource(MICROPROFILE_CONFIG_PROPERTIES_FILE, javaProject));
	}

	/**
	 * Returns the value of this property or <code>defaultValue</code> if it is not
	 * defined in this project
	 *
	 * @param propertyKey  the property to get with the profile included, in the
	 *                     format used by microprofile-config.properties
	 * @param defaultValue the value to return if the value for the property is not
	 *                     defined in this project
	 * @return the value of this property or <code>defaultValue</code> if it is not
	 *         defined in this project
	 */
	public String getProperty(String propertyKey, String defaultValue) {
		for (IConfigSource configSource : configSources) {
			String propertyValue = configSource.getProperty(propertyKey);
			if (propertyValue != null) {
				return propertyValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Returns the value of this property or null if it is not defined in this
	 * project
	 *
	 * @param propertyKey the property to get with the profile included, in the
	 *                    format used by microprofile-config.properties
	 * @return the value of this property or null if it is not defined in this
	 *         project
	 */
	public String getProperty(String propertyKey) {
		return getProperty(propertyKey, null);
	}

	public Integer getPropertyAsInteger(String key, Integer defaultValue) {
		for (IConfigSource configSource : configSources) {
			Integer property = configSource.getPropertyAsInt(key);
			if (property != null) {
				return property;
			}
		}
		return defaultValue;
	}

	/**
	 * Returns a list of the property information (values for each profile and where
	 * they are assigned) for a given property
	 *
	 * @param propertyKey the name of the property to collect the values for
	 * @return a list of all values for properties and different profiles that are
	 *         defined in this project
	 */
	public List<MicroProfileConfigPropertyInformation> getPropertyInformations(String propertyKey) {
		// Use a map to override property values
		// eg. if application.yaml defines a value for a property it should override the
		// value defined in application.properties
		Map<String, MicroProfileConfigPropertyInformation> propertyToInfoMap = new HashMap<>();
		IConfigSource configSource;
		// Go backwards so that application.properties replaces
		// microprofile-config.properties, etc.
		for (int i = configSources.size() - 1; i >= 0; i--) {
			configSource = configSources.get(i);
			propertyToInfoMap.putAll(configSource.getPropertyInformations(propertyKey));
		}
		return propertyToInfoMap.values().stream() //
				.sorted((a, b) -> {
					return a.getPropertyNameWithProfile().compareTo(b.getPropertyNameWithProfile());
				}) //
				.collect(Collectors.toList());
	}

}