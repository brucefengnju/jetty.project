// ========================================================================
// Copyright (c) 2006-2009 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at 
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses. 
// ========================================================================

package org.eclipse.jetty.plus.webapp;

import java.util.Random;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import org.eclipse.jetty.plus.annotation.InjectionCollection;
import org.eclipse.jetty.plus.annotation.LifeCycleCallbackCollection;
import org.eclipse.jetty.plus.annotation.RunAsCollection;
import org.eclipse.jetty.plus.jndi.Transaction;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.webapp.Fragment;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlProcessor;


/**
 * Configuration
 *
 *
 */
public class Configuration implements org.eclipse.jetty.webapp.Configuration
{

    private Integer _key;
    
    public void preConfigure (WebAppContext context)
    throws Exception
    {      
        LifeCycleCallbackCollection callbacks = new LifeCycleCallbackCollection();
        context.setAttribute(LifeCycleCallbackCollection.LIFECYCLE_CALLBACK_COLLECTION, callbacks);
        InjectionCollection injections = new InjectionCollection();
        context.setAttribute(InjectionCollection.INJECTION_COLLECTION, injections);
        RunAsCollection runAsCollection = new RunAsCollection();
        context.setAttribute(RunAsCollection.RUNAS_COLLECTION, runAsCollection);  
        WebAppDecorator decorator = new WebAppDecorator(context);
        context.setDecorator(decorator);
    }
   
  

    public void configure (WebAppContext context)
    throws Exception
    {
        bindUserTransaction(context);
        
        WebXmlProcessor webXmlProcessor = (WebXmlProcessor)context.getAttribute(WebXmlProcessor.WEB_PROCESSOR); 
        if (webXmlProcessor == null)
           throw new IllegalStateException ("No processor for web xml");
        
        PlusDescriptorProcessor plusProcessor = new PlusDescriptorProcessor(webXmlProcessor);
        webXmlProcessor.process(webXmlProcessor.getWebDefault(), plusProcessor);
        webXmlProcessor.process(webXmlProcessor.getWebXml(), plusProcessor);
     

        //Process plus-elements of each descriptor
        for (Fragment frag: webXmlProcessor.getFragments())
        {
            webXmlProcessor.process(frag, plusProcessor);
        }

        //process the override-web.xml descriptor
        webXmlProcessor.process(webXmlProcessor.getOverrideWeb(), plusProcessor);
    }
    
    public void postConfigure(WebAppContext context) throws Exception
    {
        //lock this webapp's java:comp namespace as per J2EE spec
        lockCompEnv(context);
    
        context.setAttribute(LifeCycleCallbackCollection.LIFECYCLE_CALLBACK_COLLECTION, null);
        context.setAttribute(InjectionCollection.INJECTION_COLLECTION, null);
        context.setAttribute(RunAsCollection.RUNAS_COLLECTION, null); 
    }
    
    public void deconfigure (WebAppContext context)
    throws Exception
    {
        unlockCompEnv(context);
        _key = null;
    }
    
    public void bindUserTransaction (WebAppContext context)
    throws Exception
    {
        try
        {
           Transaction.bindToENC();
        }
        catch (NameNotFoundException e)
        {
            Log.info("No Transaction manager found - if your webapp requires one, please configure one.");
        }
    }
    
 
  
    protected void lockCompEnv (WebAppContext wac)
    throws Exception
    {
        ClassLoader old_loader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(wac.getClassLoader());
        try
        {
            Random random = new Random ();
            _key = new Integer(random.nextInt());
            Context context = new InitialContext();
            Context compCtx = (Context)context.lookup("java:comp");
            compCtx.addToEnvironment("org.eclipse.jndi.lock", _key);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(old_loader);
        }
    }
    
    protected void unlockCompEnv (WebAppContext wac)
    throws Exception
    {
        if (_key!=null)
        {
            ClassLoader old_loader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(wac.getClassLoader());

            try
            {
                Context context = new InitialContext();
                Context compCtx = (Context)context.lookup("java:comp");
                compCtx.addToEnvironment("org.eclipse.jndi.unlock", _key); 
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(old_loader);
            }
        }
    }
}
