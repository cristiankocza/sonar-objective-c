/*
 * Sonar Objective-C Plugin
 * Copyright (C) 2012 OCTO Technology
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.objectivec.violations;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.slf4j.LoggerFactory;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.StaxParser;

final class OCLintParser {
    private final Project project;
    private final SensorContext context;

    public OCLintParser(final Project p, final SensorContext c) {
        project = p;
        context = c;
    }

    public Collection<Violation> parseReport(final File file) {
        Collection<Violation> result;
        try {
            final InputStream reportStream = new FileInputStream(file);
            result = parseReport(reportStream);
            reportStream.close();
        } catch (final IOException e) {
            LoggerFactory.getLogger(getClass()).error(
                    "Error processing file named {}", file, e);
            result = new ArrayList<Violation>();
        }
        return result;
    }

    public Collection<Violation> parseReport(final InputStream inputStream) {
        Collection<Violation> violations = new ArrayList<Violation>();
        try {
            final StaxParser parser = new StaxParser(
                    new OCLintXMLStreamHandler(violations, project, context));
            parser.parse(inputStream);
            Collection<Violation> tmpViolations = new ArrayList<Violation>();
            Collection<String> reportedViolations = new ArrayList<String>();
            //oclint will sometimes not specify a message for a violation, taking one from the rule
            //also oclint will sometimes report the same violation multiple times, making sure we remove the duplicates
            for(Violation violation : violations){
                String message = violation.getMessage();
                if(violation.getRule() != null && (message == null || message.trim().length() == 0)){
                    message = violation.getRule().getDescription();
                    if(message == null || message.trim().length() == 0) message = violation.getRule().getKey();
                    if(message != null && message.trim().length() > 0){
                        violation.setMessage(message.trim());
                    }
                }
                String key = String.format("%s-%d-%s",violation.getResource().getKey(),violation.hasLineId()?violation.getLineId():-1,violation.getRule().getKey());
                if(!reportedViolations.contains(key)){
                    reportedViolations.add(key);
                    tmpViolations.add(violation);
                }
            }
            violations = tmpViolations;
            LoggerFactory.getLogger(getClass()).error(
                    "Reporting {} violations.", violations.size());
        } catch (final XMLStreamException e) {
            LoggerFactory.getLogger(getClass()).error(
                    "Error while parsing XML stream.", e);
        }
        return violations;
    }

}
