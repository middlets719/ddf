/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.itests.common;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;

import ddf.security.service.SecurityManager;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.security.Security;

/** Runs the ServiceManager methods as the system subject */
public class ServiceManagerProxy implements InvocationHandler {

  private Security security;

  private ServiceManager serviceManager;

  ServiceManagerProxy(ServiceManager serviceManager, Security security) {
    this.serviceManager = serviceManager;
    this.security = security;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // wait until the security manager is available otherwise the getSystemSubject command will fail
    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(AbstractIntegrationTest.GENERIC_TIMEOUT_SECONDS, SECONDS)
        .until(() -> serviceManager.getServiceReference(SecurityManager.class) != null);

    RetryPolicy<Subject> retryPolicy =
        RetryPolicy.<Subject>builder()
            .withMaxRetries(10)
            .withDelay(Duration.ofSeconds(1))
            .handleResult(null)
            .build();
    Subject subject =
        Failsafe.with(retryPolicy).get(() -> security.runAsAdmin(security::getSystemSubject));
    return subject.execute(() -> method.invoke(serviceManager, args));
  }
}
