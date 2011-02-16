/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operations.common;


import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Locale;

import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Handler for the root resource add-namespace operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class NamespaceAddHandler implements ModelUpdateOperationHandler, DescriptionProvider {

    public static final String OPERATION_NAME = "add-namespace";

    public static final NamespaceAddHandler INSTANCE = new NamespaceAddHandler();

    public static ModelNode getAddNamespaceOperation(ModelNode address, Property namespace) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        op.get(NAMESPACE).set(namespace);
        return op;
    }

    private final ParameterValidator typeValidator = new ModelTypeValidator(ModelType.PROPERTY);

    /**
     * Create the AddNamespaceHandler
     */
    private NamespaceAddHandler() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) throws OperationFailedException {
        try {
            ModelNode param = operation.get(NAMESPACE);
            ModelNode namespaces = context.getSubModel().get(NAMESPACES);
            String failure = validate(param, namespaces);
            if (failure != null) {
                throw new OperationFailedException(new ModelNode().set(failure));
            }

            Property prop = param.asProperty();
            namespaces.add(prop.getName(), prop.getValue());
            ModelNode compensating = NamespaceRemoveHandler.getRemoveNamespaceOperation(operation.get(OP_ADDR), param.asProperty().getName());
            resultHandler.handleResultComplete();
            return new BasicOperationResult(compensating);
        }
        catch (Exception e) {
            throw new OperationFailedException(new ModelNode().set(e.getLocalizedMessage()));
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return CommonDescriptions.getAddNamespaceOperation(locale);
    }

    private String validate(ModelNode param, ModelNode namespaces) {
        String failure = typeValidator.validateParameter(NAMESPACE, param);
        String name = param.asProperty().getName();
        if (failure == null && namespaces.isDefined()) {
            for (ModelNode node : namespaces.asList()) {
                if (name.equals(node.asProperty().getName())) {
                    failure = "Namespace with prefix " + name + " already registered with schema URI " + node.asProperty().getValue().asString();
                }
            }
        }
        return failure;
    }

}
