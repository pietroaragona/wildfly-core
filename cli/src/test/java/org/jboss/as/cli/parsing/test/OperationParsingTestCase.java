/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.cli.parsing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Set;

import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.completion.mock.MockCommandContext;
import org.jboss.as.cli.operation.CommandLineParser;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.OperationRequestAddress;
import org.jboss.as.cli.operation.OperationRequestAddress.Node;
import org.jboss.as.cli.operation.impl.DefaultCallbackHandler;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestAddress;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestParser;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

/**
 *
 * @author Alexey Loubyansky
 */
public class OperationParsingTestCase {

    private CommandLineParser parser = DefaultOperationRequestParser.INSTANCE;

    @Test
    public void testOperationNameEndsWithDash() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler(false);

        parse("/subsystem=threads/thread-factory=*:validate-", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("validate-", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
/*        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());
*/    }

    @Test
    public void testOperationNameOnly() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parse("/subsystem=logging:read-resource", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());
    }

    @Test
    public void testOperationNameWithPrefix() throws Exception {

        OperationRequestAddress prefix = new DefaultOperationRequestAddress();
        prefix.toNodeType("subsystem");
        DefaultCallbackHandler handler = new DefaultCallbackHandler(prefix);

        parse("./logging:read-resource", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());
    }

    @Test
    public void testNoOperation() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        try {
            parse("./subsystem=logging:", handler);
        } catch(OperationFormatException e) {
        }

        assertTrue(handler.hasAddress());
        assertFalse(handler.hasOperationName());
        assertFalse(handler.hasProperties());
        assertTrue(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());
    }

    @Test
    public void testOperationWithArguments() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parse("/subsystem=logging:read-resource(recursive=true)", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());

        Set<String> args = handler.getPropertyNames();
        assertEquals(1, args.size());
        assertTrue(args.contains("recursive"));
        assertEquals("true", handler.getPropertyValue("recursive"));
    }

    @Test
    public void testComposite() throws Exception {

        final String op = "composite";
        final String propName = "steps";
        final String propValue = "[{\"operation\"=>\"add-system-property\",\"name\"=>\"test\",\"value\"=\"newValue\"},{\"operation\"=>\"add-system-property\",\"name\"=>\"test2\",\"value\"=>\"test2\"}]";

        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parse(':' + op + '(' + propName + '=' + propValue + ')', handler);

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("composite", handler.getOperationName());

        Set<String> args = handler.getPropertyNames();
        assertEquals(1, args.size());
        assertTrue(args.contains(propName));
        assertEquals(propValue, handler.getPropertyValue(propName));
    }

    @Test
    public void testOperationWithArgumentsAndWhitespaces() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parse("   / subsystem  =  logging  :  read-resource  ( recursive = true , another = \"   \" )   ", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("read-resource", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());

        Set<String> args = handler.getPropertyNames();
        assertEquals(2, args.size());
        assertTrue(args.contains("recursive"));
        assertEquals("true", handler.getPropertyValue("recursive"));
        assertTrue(args.contains("another"));
        assertEquals("\"   \"", handler.getPropertyValue("another"));
    }

    @Test
    public void testOperationEscapedQuotesInArgumentValues() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parse("/subsystem=logging/console-handler=CONSOLE:write-attribute(name=filter-spec, value=\"substituteAll(\\\"JBAS\\\",\\\"DUMMY\\\")\")", handler);

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("write-attribute", handler.getOperationName());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertTrue(i.hasNext());
        node = i.next();
        assertEquals("console-handler", node.getType());
        assertEquals("CONSOLE", node.getName());
        assertFalse(i.hasNext());

        Set<String> args = handler.getPropertyNames();
        assertEquals(2, args.size());
        assertTrue(args.contains("name"));
        assertEquals("filter-spec", handler.getPropertyValue("name"));
        assertTrue(args.contains("value"));
        assertEquals("\"substituteAll(\\\"JBAS\\\",\\\"DUMMY\\\")\"", handler.getPropertyValue("value"));
    }

    @Test
    public void testOpenQuote() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler(false);

        try {
            handler.parse(null, "./subsystem=logging:add(a=\"", null);
        } catch(OperationFormatException e) {
        }

        assertTrue(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        OperationRequestAddress address = handler.getAddress();
        Iterator<Node> i = address.iterator();
        assertTrue(i.hasNext());
        Node node = i.next();
        assertEquals("subsystem", node.getType());
        assertEquals("logging", node.getName());
        assertFalse(i.hasNext());

        assertEquals("add", handler.getOperationName());

        assertEquals("a", handler.getLastParsedPropertyName());
        assertEquals("\"", handler.getLastParsedPropertyValue());
    }

    @Test
    public void testDMREqualsAsParamEquals() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        parse(":add(keystore=>{password=1234test,url=/Users/xxx/clientcert.jks})", handler);

        assertFalse(handler.hasAddress());
        assertTrue(handler.hasOperationName());
        assertTrue(handler.hasProperties());
        assertFalse(handler.endsOnAddressOperationNameSeparator());
        assertFalse(handler.endsOnPropertyListStart());
        assertFalse(handler.endsOnPropertySeparator());
        assertFalse(handler.endsOnPropertyValueSeparator());
        assertFalse(handler.endsOnNodeSeparator());
        assertFalse(handler.endsOnNodeTypeNameSeparator());
        assertFalse(handler.isRequestComplete());

        assertEquals("add", handler.getOperationName());

        Set<String> args = handler.getPropertyNames();
        assertEquals(1, args.size());
        assertTrue(args.contains("keystore"));
        assertEquals(">{password=1234test,url=/Users/xxx/clientcert.jks}", handler.getPropertyValue("keystore"));
        final ModelNode request = handler.toOperationRequest(new MockCommandContext());
        final ModelNode keystoreDmr = request.get("keystore");
        assertTrue(keystoreDmr.isDefined());
        assertEquals(ModelType.OBJECT, keystoreDmr.getType());
        final Set<String> props = keystoreDmr.keys();
        assertEquals(2, props.size());

        // this name is not alphanumeric but it's still a value of the CLI operation parameter
        // and the CLI at the moment does not attempt to validate values
        assertTrue(props.contains(">{password"));
        assertTrue(props.contains("url"));
        assertEquals("1234test", keystoreDmr.get(">{password").asString());
        assertEquals("/Users/xxx/clientcert.jks}", keystoreDmr.get("url").asString());
    }

    @Test
    public void testOperationNameAfterNo() throws Exception {

        DefaultCallbackHandler handler = new DefaultCallbackHandler();

        try {
            parse("/subsystem=logging/logger:read-resource", handler);
            fail("Shouldn't allow parsing of operation names following incomplete node paths");
        } catch(OperationFormatException expected) {
        }

        try {
            parse("/subsystem=logging/logger=:read-resource", handler);
            fail("Shouldn't allow parsing of operation names following incomplete node paths");
        } catch(OperationFormatException expected) {
        }
    }

    @Test
    public void testImplicitValuesForBooleanProperties() throws Exception {
        DefaultCallbackHandler handler = new DefaultCallbackHandler();
        parse("/subsystem=logging:read-resource(prop1,prop2,prop3=toto,prop4)",
                handler);
        assertTrue(handler.getPropertyNames().size() == 4);
        assertTrue(handler.getPropertyNames().contains("prop1"));
        assertTrue(handler.getPropertyValue("prop1") == null);
        assertTrue(handler.getPropertyNames().contains("prop2"));
        assertTrue(handler.getPropertyValue("prop2") == null);
        assertTrue(handler.getPropertyNames().contains("prop3"));
        assertTrue(handler.getPropertyValue("prop3").equals("toto"));
        assertTrue(handler.getPropertyNames().contains("prop4"));
        assertTrue(handler.getPropertyValue("prop4") == null);
    }

    protected void parse(String opReq, DefaultCallbackHandler handler) throws CommandFormatException {
        parser.parse(opReq, handler);
    }
}
