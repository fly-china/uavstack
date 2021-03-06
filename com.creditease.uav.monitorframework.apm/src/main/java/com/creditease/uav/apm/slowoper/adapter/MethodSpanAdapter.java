/*-
 * <<
 * UAVStack
 * ==
 * Copyright (C) 2016 - 2017 UAVStack
 * ==
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * >>
 */

package com.creditease.uav.apm.slowoper.adapter;

import com.creditease.agent.helpers.DataConvertHelper;
import com.creditease.agent.helpers.EncodeHelper;
import com.creditease.monitor.UAVServer;
import com.creditease.uav.apm.invokechain.span.Span;
import com.creditease.uav.apm.invokechain.spi.InvokeChainAdapter;
import com.creditease.uav.apm.invokechain.spi.InvokeChainConstants;
import com.creditease.uav.apm.invokechain.spi.InvokeChainContext;
import com.creditease.uav.apm.slowoper.spi.SlowOperConstants;
import com.creditease.uav.apm.slowoper.spi.SlowOperContext;

public class MethodSpanAdapter extends InvokeChainAdapter {

    @Override
    public void beforePreCap(InvokeChainContext context, Object[] args) {

        // do noting
    }

    @Override
    public void afterPreCap(InvokeChainContext context, Object[] args) {

        if (UAVServer.instance().isExistSupportor("com.creditease.uav.apm.supporters.SlowOperSupporter")) {

            String storekey = (String) context.get(InvokeChainConstants.METHOD_SPAN_STOREKEY);
            Span span = (Span) context.get(storekey);

            SlowOperContext slowOperContext = new SlowOperContext();
            slowOperContext.put(SlowOperConstants.PROTOCOL_METHOD_PARAMS, parseParams(args));

            Object params[] = { span, slowOperContext };
            // 由于使用Endpoint Info不能区分方法级，故此处使用常量
            UAVServer.instance().runSupporter("com.creditease.uav.apm.supporters.SlowOperSupporter", "runCap",
                    SlowOperConstants.SLOW_OPER_METHOD, InvokeChainConstants.CapturePhase.PRECAP, context, params);
        }
    }

    @Override
    public void beforeDoCap(InvokeChainContext context, Object[] args) {

        // do nothing
    }

    @Override
    public void afterDoCap(InvokeChainContext context, Object[] args) {

        if (UAVServer.instance().isExistSupportor("com.creditease.uav.apm.supporters.SlowOperSupporter")) {

            String storekey = (String) context.get(InvokeChainConstants.METHOD_SPAN_STOREKEY);

            if (storekey == null) {
                return;
            }

            Span span = (Span) context.get(storekey);

            if (span == null) {
                return;
            }

            SlowOperContext slowOperContext = new SlowOperContext();
            slowOperContext.put(SlowOperConstants.PROTOCOL_METHOD_RETURN, parseReturn(args));

            Object params[] = { span, slowOperContext };
            UAVServer.instance().runSupporter("com.creditease.uav.apm.supporters.SlowOperSupporter", "runCap",
                    SlowOperConstants.SLOW_OPER_METHOD, InvokeChainConstants.CapturePhase.DOCAP, context, params);
        }
    }

    /**
     * 解析入参
     * 
     * @param args
     * @return
     */
    private String parseParams(Object[] args) {

        if (args == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (Object temp : args) {
            if (temp == null) {
                temp = "null";
            }
            String tempStr = temp.toString();
            // 限定采集的协议体的大小 当length为小于0时不限制长度，为0时则直接为空（不去获取）
            int methodParamsLength = DataConvertHelper.toInt(System.getProperty("com.creditease.uav.ivcdat.method.req"),
                    2000);
            if (tempStr.toString().length() > methodParamsLength && methodParamsLength > 0) {
                tempStr = tempStr.substring(0, methodParamsLength);
            }
            else if (methodParamsLength == 0) {
                tempStr = "";
            }
            tempStr = EncodeHelper.urlEncode(tempStr);
            stringBuilder.append(tempStr.length() + ";" + tempStr.toString() + ";");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }

    /**
     * 解析出参
     * 
     * @param arg
     * @return
     */
    private String parseReturn(Object[] args) {

        Object temp = args[0];
        if (temp == null) {
            temp = "null";
        }
        String tempStr = temp.toString();
        // 限定采集的协议体的大小 当length为小于0时不限制长度，为0时则直接为空（不去获取）
        int methodReturnLength = DataConvertHelper.toInt(System.getProperty("com.creditease.uav.ivcdat.method.ret"),
                2000);
        if (tempStr.toString().length() > methodReturnLength && methodReturnLength > 0) {
            tempStr = tempStr.substring(0, methodReturnLength);
        }
        else if (methodReturnLength == 0) {
            tempStr = "";
        }
        return EncodeHelper.urlEncode(tempStr);
    }
}
