/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs;

import java.io.IOException;
import java.net.InetSocketAddress;

import junit.framework.Assert;

import org.apache.hadoop.hdfs.protocol.ClientProtocol;
import org.apache.hadoop.hdfs.protocolPB.ClientDatanodeProtocolTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.ClientNamenodeProtocolTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.DatanodeProtocolClientSideTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.GetUserMappingsProtocolClientSideTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.InterDatanodeProtocolTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.JournalProtocolTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.NamenodeProtocolTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.RefreshAuthorizationPolicyProtocolClientSideTranslatorPB;
import org.apache.hadoop.hdfs.protocolPB.RefreshUserMappingsProtocolClientSideTranslatorPB;
import org.apache.hadoop.hdfs.server.protocol.JournalProtocol;
import org.apache.hadoop.hdfs.server.protocol.NamenodeProtocol;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.RefreshUserMappingsProtocol;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.authorize.RefreshAuthorizationPolicyProtocol;
import org.apache.hadoop.tools.GetUserMappingsProtocol;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cases to verify that client side translators correctly implement the
 * isMethodSupported method in ProtocolMetaInterface.
 */
public class TestIsMethodSupported {
  private static MiniDFSCluster cluster = null;
  private static HdfsConfiguration conf = new HdfsConfiguration();
  private static InetSocketAddress nnAddress = null;
  private static InetSocketAddress dnAddress = null;
  
  @BeforeClass
  public static void setUp() throws Exception {
    cluster = (new MiniDFSCluster.Builder(conf))
        .numDataNodes(1).build();
    nnAddress = cluster.getNameNode().getNameNodeAddress();
    dnAddress = new InetSocketAddress(cluster.getDataNodes().get(0)
        .getDatanodeId().getHost(), cluster.getDataNodes().get(0).getIpcPort());
  }

  @AfterClass
  public static void tearDown() throws Exception {
    if (cluster != null) {
      cluster.shutdown();
    }
  }

  @Test
  public void testNamenodeProtocol() throws IOException {
    NamenodeProtocolTranslatorPB translator =
        (NamenodeProtocolTranslatorPB) NameNodeProxies.createNonHAProxy(conf,
            nnAddress, NamenodeProtocol.class, UserGroupInformation.getCurrentUser(),
            true).getProxy();
    boolean exists = translator.isMethodSupported("rollEditLog");
    Assert.assertTrue(exists);
    exists = translator.isMethodSupported("bogusMethod");
    Assert.assertFalse(exists);
  }
  
  @Test
  public void testDatanodeProtocol() throws IOException {
    DatanodeProtocolClientSideTranslatorPB translator = 
        new DatanodeProtocolClientSideTranslatorPB(nnAddress, conf);
    Assert.assertTrue(translator.isMethodSupported("sendHeartbeat"));
  }
  
  @Test
  public void testClientDatanodeProtocol() throws IOException {
    ClientDatanodeProtocolTranslatorPB translator = 
        new ClientDatanodeProtocolTranslatorPB(nnAddress, 
            UserGroupInformation.getCurrentUser(), conf,
        NetUtils.getDefaultSocketFactory(conf));
    //Namenode doesn't implement ClientDatanodeProtocol
    Assert.assertFalse(translator.isMethodSupported("refreshNamenodes"));
    
    translator = new ClientDatanodeProtocolTranslatorPB(
        dnAddress, UserGroupInformation.getCurrentUser(), conf,
        NetUtils.getDefaultSocketFactory(conf));
    Assert.assertTrue(translator.isMethodSupported("refreshNamenodes"));
  }
  
  @Test
  public void testClientNamenodeProtocol() throws IOException {
    ClientNamenodeProtocolTranslatorPB translator = 
        (ClientNamenodeProtocolTranslatorPB) NameNodeProxies.createNonHAProxy(
            conf, nnAddress, ClientProtocol.class,
            UserGroupInformation.getCurrentUser(), true).getProxy();
    Assert.assertTrue(translator.isMethodSupported("mkdirs"));
  }
  
  @Test
  public void tesJournalProtocol() throws IOException {
    JournalProtocolTranslatorPB translator = (JournalProtocolTranslatorPB)
        NameNodeProxies.createNonHAProxy(conf, nnAddress, JournalProtocol.class,
            UserGroupInformation.getCurrentUser(), true).getProxy();
    //Nameode doesn't implement JournalProtocol
    Assert.assertFalse(translator.isMethodSupported("startLogSegment"));
  }
  
  @Test
  public void testInterDatanodeProtocol() throws IOException {
    InterDatanodeProtocolTranslatorPB translator = 
        new InterDatanodeProtocolTranslatorPB(
            nnAddress, UserGroupInformation.getCurrentUser(), conf,
            NetUtils.getDefaultSocketFactory(conf), 0);
    //Not supported at namenode
    Assert.assertFalse(translator.isMethodSupported("initReplicaRecovery"));
    
    translator = new InterDatanodeProtocolTranslatorPB(
        dnAddress, UserGroupInformation.getCurrentUser(), conf,
        NetUtils.getDefaultSocketFactory(conf), 0);
    Assert.assertTrue(translator.isMethodSupported("initReplicaRecovery"));
  }
  
  @Test
  public void testGetUserMappingsProtocol() throws IOException {
    GetUserMappingsProtocolClientSideTranslatorPB translator = 
        (GetUserMappingsProtocolClientSideTranslatorPB)
        NameNodeProxies.createNonHAProxy(conf, nnAddress,
            GetUserMappingsProtocol.class, UserGroupInformation.getCurrentUser(),
            true).getProxy();
    Assert.assertTrue(translator.isMethodSupported("getGroupsForUser"));
  }
  
  @Test
  public void testRefreshAuthorizationPolicyProtocol() throws IOException {
    RefreshAuthorizationPolicyProtocolClientSideTranslatorPB translator = 
      (RefreshAuthorizationPolicyProtocolClientSideTranslatorPB)
      NameNodeProxies.createNonHAProxy(conf, nnAddress,
          RefreshAuthorizationPolicyProtocol.class,
          UserGroupInformation.getCurrentUser(), true).getProxy();
    Assert.assertTrue(translator.isMethodSupported("refreshServiceAcl"));
  }
  
  @Test
  public void testRefreshUserMappingsProtocol() throws IOException {
    RefreshUserMappingsProtocolClientSideTranslatorPB translator =
        (RefreshUserMappingsProtocolClientSideTranslatorPB)
        NameNodeProxies.createNonHAProxy(conf, nnAddress,
            RefreshUserMappingsProtocol.class,
            UserGroupInformation.getCurrentUser(), true).getProxy();
    Assert.assertTrue(
        translator.isMethodSupported("refreshUserToGroupsMappings"));
  }
}
