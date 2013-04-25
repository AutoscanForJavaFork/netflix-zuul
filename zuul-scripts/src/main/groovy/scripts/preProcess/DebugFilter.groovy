/*
 * Copyright 2013 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */
package scripts.preProcess

import com.netflix.zuul.context.NFRequestContext
import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicPropertyFactory
import com.netflix.zuul.groovy.ZuulFilter
import com.netflix.zuul.context.RequestContext
import com.netflix.config.DynamicStringProperty

class DebugFilter extends ZuulFilter {

    static final DynamicBooleanProperty proxyDebug = DynamicPropertyFactory.getInstance().getBooleanProperty("zuul.debug.request", false)
    static final DynamicStringProperty debugParameter = DynamicPropertyFactory.getInstance().getStringProperty("zuul.debug.parameter", "debugParameter")

    @Override
    String filterType() {
        return 'pre'
    }

    @Override
    int filterOrder() {
        return 1
    }

    boolean shouldFilter() {

        if("true".equals(NFRequestContext.currentContext.getRequest().getParameter(debugParameter.get()))) return true;
        return proxyDebug.get();

    }

    Object run() {
        RequestContext.getCurrentContext().setDebugRequest(true)
        RequestContext.getCurrentContext().setDebugProxy(true)
        return null;

    }


}


