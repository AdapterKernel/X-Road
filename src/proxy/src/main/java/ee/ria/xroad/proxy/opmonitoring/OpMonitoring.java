/**
 * The MIT License
 * Copyright (c) 2016 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.proxy.opmonitoring;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import lombok.extern.slf4j.Slf4j;

import ee.ria.xroad.common.SystemProperties;
import ee.ria.xroad.common.opmonitoring.AbstractOpMonitoringBuffer;
import ee.ria.xroad.common.opmonitoring.OpMonitoringData;

/**
 * Contains method for storing operational monitoring data.
 */
@Slf4j
public final class OpMonitoring {

    public static final String OP_MONITORING_BUFFER = "OpMonitoringBuffer";

    public static final String OP_MONITORING_BUFFER_IMPL_CLASS =
            SystemProperties.PREFIX + "proxy.opMonitoringBufferImpl";

    private static ActorRef opMonitoringBuffer;

    private OpMonitoring() {
    }

    /**
     * Initializes the operational monitoring using the provided actor system.
     * @param actorSystem the actor system
     * @throws Exception if initialization fails
     */
    public static void init(ActorSystem actorSystem) throws Exception {
        Class<? extends AbstractOpMonitoringBuffer> clazz =
                getOpMonitoringManagerImpl();

        log.trace("Using implementation class: {}", clazz);

        opMonitoringBuffer = actorSystem.actorOf(Props.create(clazz),
                OP_MONITORING_BUFFER);
    }

    /**
     * Store the operational monitoring data.
     */
    public static void store(OpMonitoringData data) {
        log.trace("store()");

        try {
            tell(data);
        } catch (Throwable t) {
            log.error("Storing operational monitoring data failed", t);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends AbstractOpMonitoringBuffer>
            getOpMonitoringManagerImpl() {
        String opMonitoringBufferImplClassName = System.getProperty(
                OP_MONITORING_BUFFER_IMPL_CLASS,
                NullOpMonitoringBuffer.class.getName());

        try {
            Class<?> clazz = Class.forName(opMonitoringBufferImplClassName);

            return (Class<? extends AbstractOpMonitoringBuffer>) clazz;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "Unable to load operational monitoring buffer impl: "
                    + opMonitoringBufferImplClassName, e);
        }
    }

    private static void tell(Object message) throws Exception {
        opMonitoringBuffer.tell(message, ActorRef.noSender());
    }
}
