/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.util.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a public API that can change at any time (even in minor/bugfix releases).
 *
 * Usage guidelines:
 *
 * <ol>
 *     <li>Is not needed for things located in *.internal.* packages</li>
 *     <li>Only public accessible classes/interfaces must be annotated</li>
 *     <li>If this annotation is not present the API is considered stable and so no backward compatibility can be
 *         broken in a non-major release!</li>
 * </ol>
 *
 指示可以随时更改的公共API(即使是在小/bugfix版本中)。使用指南:
 位于*.internal.* package里面的东西不需要吗
 只有公共访问类/接口必须被注释。
 如果没有此注释，则认为API是稳定的，因此在非主流版本中不能破坏向后兼容性!
 */
@Retention(RetentionPolicy.SOURCE)
@Target({
        ElementType.ANNOTATION_TYPE,
        ElementType.CONSTRUCTOR,
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PACKAGE,
        ElementType.TYPE
})
@Documented
public @interface UnstableApi {
}
