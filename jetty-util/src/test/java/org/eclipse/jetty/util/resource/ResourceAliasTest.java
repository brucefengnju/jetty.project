//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.util.resource;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import junit.framework.Assert;

import org.eclipse.jetty.toolchain.test.FS;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.toolchain.test.TestingDir;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class ResourceAliasTest
{
    static File __dir;
    
    @BeforeClass
    public static void beforeClass()
    {
        __dir=MavenTestingUtils.getTargetTestingDir("RAT");
    }
    
    @Before
    public void before()
    {
        FS.ensureDirExists(__dir);
        FS.ensureEmpty(__dir);
    }
    
    
    /* ------------------------------------------------------------ */
    @Test
    public void testNullCharEndingFilename() throws Exception
    {
        File file=new File(__dir,"test.txt");
        assertFalse(file.exists());
        file.createNewFile();
        assertTrue(file.exists());

        File file0=new File(__dir,"test.txt\0");
        if (!file0.exists())
            return;  // this file system does not suffer this problem
        
        assertTrue(file0.exists()); // This is an alias!

        Resource dir = Resource.newResource(__dir); 
        
        // Test not alias paths
        Resource resource = Resource.newResource(file);
        assertTrue(resource.exists());
        assertNull(resource.getAlias());
        resource = Resource.newResource(file.getAbsoluteFile());
        assertTrue(resource.exists());
        assertNull(resource.getAlias());
        resource = Resource.newResource(file.toURI());
        assertTrue(resource.exists());
        assertNull(resource.getAlias());
        resource = Resource.newResource(file.toURI().toString());
        assertTrue(resource.exists());
        assertNull(resource.getAlias());
        resource = dir.addPath("test.txt");
        assertTrue(resource.exists());
        assertNull(resource.getAlias());
        
        
        // Test alias paths
        resource = Resource.newResource(file0);
        assertTrue(resource.exists());
        assertNotNull(resource.getAlias());
        resource = Resource.newResource(file0.getAbsoluteFile());
        assertTrue(resource.exists());
        assertNotNull(resource.getAlias());
        resource = Resource.newResource(file0.toURI());
        assertTrue(resource.exists());
        assertNotNull(resource.getAlias());
        resource = Resource.newResource(file0.toURI().toString());
        assertTrue(resource.exists());
        assertNotNull(resource.getAlias());
        
        try
        {
            resource = dir.addPath("test.txt\0");
            assertTrue(resource.exists());
            assertNotNull(resource.getAlias());
        }
        catch(MalformedURLException e)
        {
            assertTrue(true);
        }
    }
}
