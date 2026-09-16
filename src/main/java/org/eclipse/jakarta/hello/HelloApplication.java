package org.eclipse.jakarta.hello;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("rest")
@ApplicationScoped
public class HelloApplication extends Application {
    // @Path / @Provider のクラスはアーカイブから自動検出される。
}
