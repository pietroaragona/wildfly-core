/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.client.helpers.domain.ServerStatus;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.process.ProcessMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.dmr.ModelNode;

/**
 * Inventory of the managed servers.
 *
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
   */
public interface ServerInventory {

    /**
     * Gets the process name for a server
     *
     * @param serverName the name of a server in the model
     * @return the server name
     */
    String getServerProcessName(String serverName);

    /**
     * Gets the server model name for a process
     *
     * @param processName the name of the server process
     * @return the server model name
     */
    String getProcessServerName(String processName);

    /**
     * Gets information on all the running processes
     *
     * @return map of all server process names to information about the process
     */
    Map<String, ProcessInfo> determineRunningProcesses();

    /**
     * Gets information on all the running processes
     *
     * @param serversOnly {@code true} to only return the server processes
     * @return map of server process names to information about the process
     */
    Map<String, ProcessInfo> determineRunningProcesses(boolean serversOnly);

    /**
     * Get the status of the server with the given name.
     *
     * @param serverName  the server name. Cannot be {@code null}
     *
     * @return the status. Will not return {@code null}; will return {@link ServerStatus#STOPPED} for unknown servers
     */
    ServerStatus determineServerStatus(final String serverName);

    /**
     * Start the server with the given name. Note that returning from this method does not mean the server
     * is completely started; it usually will only be in the process of starting, having received all startup instructions.
     *
     * @param serverName the name of the server
     * @param domainModel the configuration model for the domain
     * @return the status of the server following the attempt to start
     */
    ServerStatus startServer(final String serverName, final ModelNode domainModel);

    /**
     * Start the server with the given name.
     *
     * @param serverName the name of the server
     * @param domainModel the configuration model for the domain
     * @param blocking whether to block until the server is started
     * @return the status of the server following the attempt to start
     */
    ServerStatus startServer(String serverName, ModelNode domainModel, boolean blocking);

    /**
     * Restart the server with the given name. Note that returning from this method does not mean the server
     * is completely started; it usually will only be in the process of starting, having received all startup instructions.
     *
     * @param serverName the name of the server
     * @param gracefulTimeout time in ms the server should allow for graceful shutdown (if supported) before terminating all services
     * @param domainModel the configuration model for the domain
     *
     * @return the status of the server following the attempt to restart
     */
    ServerStatus restartServer(String serverName, final int gracefulTimeout, final ModelNode domainModel);

    /**
     * Restart the server with the given name.
     *
     * @param serverName the name of the server
     * @param gracefulTimeout time in ms the server should allow for graceful shutdown (if supported) before terminating all services
     * @param domainModel the configuration model for the domain
     * @param blocking whether to block until the server is restarted
     * @return the status of the server following the attempt to restart
     */
    ServerStatus restartServer(String serverName, int gracefulTimeout, ModelNode domainModel, boolean blocking);

    /**
     * Stop the server with the given name. Note that returning from this method does not mean the server
     * is completely stopped; it may only be in the process of stopping.
     *
     * @param serverName the name of the server
     * @param gracefulTimeout time in ms the server should allow for graceful shutdown (if supported) before terminating all services
     *
     * @return the status of the server following the attempt to stop
     */
    ServerStatus stopServer(final String serverName, final int gracefulTimeout);

    /**
     * Stop the server with the given name.
     *
     * @param serverName the name of the server
     * @param gracefulTimeout time in ms the server should allow for graceful shutdown (if supported) before terminating all services
     * @param blocking whether to block until the server is stopped
     * @return the status of the server following the attempt to stop
     */
    ServerStatus stopServer(String serverName, int gracefulTimeout, boolean blocking);

    /**
     * Stop all servers. Note that returning from this method does not mean the servers
     * are completely stopped; they may only be in the process of stopping.
     *
     * @param gracefulTimeout time in ms a server should allow for graceful shutdown (if supported) before terminating all services
     */
    void stopServers(int gracefulTimeout);

    /**
     * Stop all servers. Note that unless {@code blockUntilStopped} is set to {@code true} returning from this method
     * does not mean the servers are completely stopped;
     *
     * @param gracefulTimeout time in ms a server should allow for graceful shutdown (if supported) before terminating all services
     * @param blockUntilStopped wait until all servers are stopped
     */
    void stopServers(int gracefulTimeout, boolean blockUntilStopped);

    /**
     * Re-establishes management communications with a server following a restart of the Host Controller process.
     *
     * @param serverName the name of the server
     * @param domainModel the configuration model for the domain
     * @param authKey the authentication key
     * @param running whether the process was running. If {@code false}, the existence of the server will be
     *                recorded but no attempt to contact it will be made
     * @param stopping whether the process is currently stopping
     */
    void reconnectServer(String serverName, ModelNode domainModel, String authKey, boolean running, boolean stopping);

    /**
     * Reload a server with the given name.
     *
     * @param serverName the name of the server
     * @blockign whether to block until the server is started
     */
    ServerStatus reloadServer(String serverName, boolean blocking);

    /**
     * Destroy a stopping server process. In case the the server is not stopping, this will attempt to stop the server
     * and this method has to be called again.
     *
     * @param serverName the server name
     */
    void destroyServer(String serverName);

    /**
     * Try to kill a server process. In case the server is not stopping, this will attempt to stop the server and this
     * method has to be called again.
     *
     * @param serverName the server name
     */
    void killServer(String serverName);

    /**
     * Gets a callback handler security services can use for handling authentication data provided by
     * a server attempting to connect with this host controller.
     *
     * @return the callback handler. Will not be {@code null}
     */
    CallbackHandler getServerCallbackHandler();

    /**
     * Notification that a channel for communication with a managed server process has been registered.
     *
     * @param serverProcessName the name of the server process
     * @param channelHandler remoting channel to use for communicating with the server
     * @return the server proxy
     */
    ProxyController serverCommunicationRegistered(String serverProcessName, ManagementChannelHandler channelHandler);

    /**
     * Notification that a server has been reconnected.
     *
     * This will also check whether a server is still in sync with the current domain model, or there were updates
     * while the server was disconnected.
     *
     * @param serverProcessName the name of the server process
     * @param channelHandler mgmt channel handler for communication with the server
     * @return {@code true} if the server is still in sync, {@code false} otherwise
     */
    boolean serverReconnected(String serverProcessName, ManagementChannelHandler channelHandler);

    /**
     * Notification that the server is started.
     *
     * @param serverProcessName the name of the server process
     */
    void serverStarted(String serverProcessName);

    /**
     * Notification that the start of a server process has failed.
     *
     * @param serverProcessName the name of the server process
     */
    void serverStartFailed(String serverProcessName);

    /**
     * Notification that a server has stopped.
     *
     * @param serverProcessName the name of the server process
     */
    void serverProcessStopped(String serverProcessName);

    /**
     * Signal the end of the PC connection, regardless of the reason.
     */
    void connectionFinished();

    /**
     * Notification that a server has been added to the process-controller.
     *
     * @param processName the process name
     */
    void serverProcessAdded(String processName);

    /**
     * Notification that a server process has been started.
     *
     * @param processName the process name
     */
    void serverProcessStarted(String processName);

    /**
     * Notification that a server has been removed from the process-controller.
     *
     * @param processName the process name
     */
    void serverProcessRemoved(String processName);

    /**
     * Notification that an operation failed on the process-controller.
     *
     * @param processName the process name
     * @param type the operation type
     */
    void operationFailed(String processName, ProcessMessageHandler.OperationType type);

    /**
     * Notification that managed server process information is available.
     *
     * @param processInfos map of process name to information about the process
     */
    void processInventory(Map<String, ProcessInfo> processInfos);

    /**
     * Await for a group of servers to be either started or stopped.
     *
     * @param serverNames the server names in the group
     * @param started whether to wait for the started, or the stopped notification
     * @throws InterruptedException
     */
    void awaitServersState(Collection<String> serverNames, boolean started);

    /**
     * Suspends a server, allowing current requests to finish and blocking any new requests
     * from starting.
     *
     * @param serverName The server name
     */
    void suspendServer(String serverName);

    /**
     * Resumes a server, allowing it to begin processing requests normally
     * @param serverName The server name
     */
    void resumeServer(String serverName);

    /**
     * Suspends and waits for the given set of servers to suspend up to the timeout
     * @param waitForServers The servers to wait for
     * @param timeoutInSeconds The maximum amount of time to wait in seconds, with -1 meaning indefinitly
     * @return <code>true</code> if all the servers suspended in time
     */
    boolean awaitServerSuspend(Set<String> waitForServers, int timeoutInSeconds);

}
