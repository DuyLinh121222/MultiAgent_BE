package com.mael.dto.discussion;

import com.mael.entity.enums.AgentType;

public record AgentView(
        String sessionAgentId,
        String agentId,
        String name,
        AgentType agentType
) {
}
