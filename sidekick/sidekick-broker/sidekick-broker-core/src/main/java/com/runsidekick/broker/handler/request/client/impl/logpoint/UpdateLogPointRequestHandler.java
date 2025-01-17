package com.runsidekick.broker.handler.request.client.impl.logpoint;

import com.runsidekick.audit.logger.annotations.Audit;
import com.runsidekick.audit.logger.services.AuditLogService;
import com.runsidekick.broker.handler.request.RequestContext;
import com.runsidekick.broker.model.LogPoint;
import com.runsidekick.broker.model.LogPointConfig;
import com.runsidekick.broker.model.request.impl.logpoint.UpdateLogPointRequest;
import com.runsidekick.broker.model.response.impl.logpoint.UpdateLogPointResponse;
import com.runsidekick.broker.proxy.ChannelInfo;
import com.runsidekick.broker.service.LogPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yasin.kalafat
 */
@Component
public class UpdateLogPointRequestHandler
        extends LogPointChangeRequestHandler<UpdateLogPointRequest, UpdateLogPointResponse> {

    public static final String REQUEST_NAME = "UpdateLogPointRequest";

    @Autowired
    private AuditLogService auditLogService;
    @Autowired
    private LogPointService logPointService;

    public UpdateLogPointRequestHandler() {
        super(REQUEST_NAME, UpdateLogPointRequest.class, UpdateLogPointResponse.class);
    }

    @Override
    @Audit(action = "UPDATE_LOGPOINT", domain = "LOGPOINT")
    public UpdateLogPointResponse handleRequest(ChannelInfo channelInfo,
                                                  UpdateLogPointRequest request,
                                                  RequestContext requestContext) {
        UpdateLogPointResponse updateLogPointResponse = new UpdateLogPointResponse();
        if (request.isPersist() && request.getLogPointId() != null) {
            LogPoint logPoint = new LogPoint();
            logPoint.setId(request.getLogPointId());
            logPoint.setClient(request.getClient());
            logPoint.setLineNo(request.getLineNo());
            logPoint.setFileName(request.getFileName());
            logPoint.setExpireSecs(request.getExpireSecs());
            logPoint.setExpireCount(request.getExpireCount());
            logPoint.setDisabled(request.isDisable());
            logPoint.setConditionExpression(request.getConditionExpression());
            logPoint.setLogExpression(request.getLogExpression());
            logPoint.setStdoutEnabled(request.isStdoutEnabled());
            logPoint.setLogLevel(request.getLogLevel());
            logPoint.setWebhookIds(request.getWebhookIds());
            logPoint.setPredefined(request.isPredefined());
            logPoint.setProbeName(request.getProbeName());

            logPointService.updateLogPoint(
                    channelInfo.getWorkspaceId(),
                    channelInfo.getUserId(),
                    request.getLogPointId(),
                    logPoint);

            LogPointConfig logPointConfig =
                    logPointService.getLogPoint(channelInfo.getWorkspaceId(), request.getLogPointId());
            updateLogPointResponse.setProbeConfig(logPointConfig);
        }

        List<String> applicationInstanceIds = new ArrayList<>(filterApplications(channelInfo.getWorkspaceId(),
                request.getLogPointId(), request.getApplications()));
        updateLogPointResponse.setApplicationInstanceIds(new ArrayList<>(applicationInstanceIds));
        updateLogPointResponse.setRequestId(request.getId());
        updateLogPointResponse.setErroneous(false);
        sendRequestToApps(channelInfo, request.getId(), requestContext.getRequestMessage(), applicationInstanceIds);
        auditLogService.getCurrentAuditLog().ifPresent(
                auditLog -> {
                    setAuditLogUserInfo(auditLog, channelInfo, request.getClient());
                    auditLog.addAuditLogField("logpointConfig", updateLogPointResponse.getProbeConfig());
                    auditLog.addAuditLogField("applicationInstanceIds", applicationInstanceIds);
                });
        return updateLogPointResponse;
    }

}
