/**
 * The MIT License
 * Copyright (c) 2015 Estonian Information System Authority (RIA), Population Register Centre (VRK)
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
package ee.ria.xroad.commonui;

import static ee.ria.xroad.common.SystemProperties.CONF_FILE_CENTER;
import static ee.ria.xroad.common.SystemProperties.CONF_FILE_SIGNER;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import ee.ria.xroad.common.SystemPropertiesLoader;

/**
 * Encapsulates actor system management in UI.
 */
public final class UIServices {

    private static final Logger LOG = LoggerFactory.getLogger(UIServices.class);

    static {
        SystemPropertiesLoader.create().withCommonAndLocal()
            .with(CONF_FILE_CENTER)
            .with(CONF_FILE_SIGNER)
            .load();
    }

    private ActorSystem actorSystem;

    /**
     * Creates the instance using the provided actor system name and
     * configuration name.
     * @param actorSystemName the actor system name
     * @param configName the configuration name
     */
    public UIServices(String actorSystemName, String configName) {
        LOG.debug("Creating ActorSystem...");

        Config config = ConfigFactory.load().getConfig(configName)
                .withFallback(ConfigFactory.load());

        LOG.debug("Akka using configuration: {}", config);
        actorSystem = ActorSystem.create(actorSystemName, config);
    }

    /**
     * @return the actor system
     */
    public ActorSystem getActorSystem() {
        return actorSystem;
    }

    /**
     * Stops the actor system.
     * @throws Exception if an error occurs
     */
    public void stop() throws Exception {
        LOG.info("stop()");

        if (actorSystem != null) {
            actorSystem.shutdown();
        }
    }
}
