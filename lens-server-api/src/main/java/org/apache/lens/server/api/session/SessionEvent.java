/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.lens.server.api.session;

import java.util.UUID;

import org.apache.lens.api.LensSessionHandle;
import org.apache.lens.server.api.events.LensEvent;

import lombok.Getter;
import lombok.ToString;

@ToString
public abstract class SessionEvent extends LensEvent {

  /**
   * The lens session handle.
   */
  @Getter
  protected final LensSessionHandle sessionHandle;

  /**
   * The id.
   */
  protected final UUID id = UUID.randomUUID();

  /**
   * Instantiates a new session event
   *
   * @param eventTime the event time
   * @param handle    the lens session handle
   * @param user      name of user
   */
  public SessionEvent(long eventTime, LensSessionHandle handle) {
    super(eventTime);
    this.sessionHandle = handle;
  }

  @Override
  public String getEventId() {
    return id.toString();
  }
}
