package io.metersphere.service;

import io.metersphere.base.domain.TestResource;
import io.metersphere.base.domain.TestResourceExample;
import io.metersphere.base.mapper.TestResourceMapper;
import io.metersphere.commons.constants.ResourceStatusEnum;
import io.metersphere.commons.exception.MSException;
import io.metersphere.commons.utils.JSON;
import io.metersphere.commons.utils.LogUtil;
import io.metersphere.controller.handler.ResultHolder;
import io.metersphere.dto.NodeDTO;
import io.metersphere.dto.TestResourcePoolDTO;
import io.metersphere.i18n.Translator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class NodeResourcePoolService {
    private final static String nodeControllerUrl = "http://%s:%s/status";

    private static final RestTemplate restTemplateWithTimeOut = new RestTemplate();

    @Resource
    private TestResourceMapper testResourceMapper;

    static {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(2000);
        httpRequestFactory.setConnectTimeout(2000);
        httpRequestFactory.setReadTimeout(1000);
        restTemplateWithTimeOut.setRequestFactory(httpRequestFactory);
    }

    public boolean validate(TestResourcePoolDTO testResourcePool) {
        if (CollectionUtils.isEmpty(testResourcePool.getResources())) {
            MSException.throwException(Translator.get("no_nodes_message"));
        }


        List<ImmutablePair<String, Integer>> ipPort = testResourcePool.getResources().stream()
                .map(resource -> {
                    NodeDTO nodeDTO = JSON.parseObject(resource.getConfiguration(), NodeDTO.class);
                    return new ImmutablePair<>(nodeDTO.getIp(), nodeDTO.getPort());
                })
                .distinct()
                .collect(Collectors.toList());
        if (ipPort.size() < testResourcePool.getResources().size()) {
            MSException.throwException(Translator.get("duplicate_node_ip_port"));
        }

        List<TestResource> resourcesFromDB = getResourcesFromDB(testResourcePool);
        List<String> resourceIdsFromDB = resourcesFromDB.stream().map(TestResource::getId).collect(Collectors.toList());
        List<String> resourceIdsFromPage = testResourcePool.getResources().stream().map(TestResource::getId).collect(Collectors.toList());
        Collection<String> deletedResources = CollectionUtils.subtract(resourceIdsFromDB, resourceIdsFromPage);
        // 删除不关联的资源节点
        deleteTestResources(deletedResources);

        testResourcePool.setStatus(ResourceStatusEnum.VALID.name());
        boolean isValid = true;
        for (TestResource resource : testResourcePool.getResources()) {
            NodeDTO nodeDTO = JSON.parseObject(resource.getConfiguration(), NodeDTO.class);
            boolean isValidate = validateNode(nodeDTO);
            if (!isValidate) {
                testResourcePool.setStatus(ResourceStatusEnum.INVALID.name());
                resource.setStatus(ResourceStatusEnum.INVALID.name());
                isValid = false;
            } else {
                resource.setStatus(ResourceStatusEnum.VALID.name());
            }
            resource.setTestResourcePoolId(testResourcePool.getId());
            updateTestResource(resource);
        }
        return isValid;
    }

    private void deleteTestResources(Collection<String> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        TestResourceExample example = new TestResourceExample();
        example.createCriteria().andIdIn(new ArrayList<>(ids));
        testResourceMapper.deleteByExample(example);
    }

    private List<TestResource> getResourcesFromDB(TestResourcePoolDTO testResourcePool) {
        TestResourceExample example = new TestResourceExample();
        example.createCriteria().andTestResourcePoolIdEqualTo(testResourcePool.getId());
        return testResourceMapper.selectByExample(example);
    }


    private boolean validateNode(NodeDTO node) {
        try {
            ResponseEntity<ResultHolder> entity = restTemplateWithTimeOut.getForEntity(String.format(nodeControllerUrl, node.getIp(), node.getPort()), ResultHolder.class);
            ResultHolder body = entity.getBody();
            if (body == null) {
                return false;
            }
            if (body.getData() != null && StringUtils.equalsIgnoreCase("OK", body.getData().toString())) {
                return true;
            }
        } catch (Exception e) {
            LogUtil.error(e.getMessage(), e);
        }
        return false;
    }

    private void updateTestResource(TestResource testResource) {
        testResource.setUpdateTime(System.currentTimeMillis());
        testResource.setCreateTime(System.currentTimeMillis());
        if (StringUtils.isBlank(testResource.getId())) {
            testResource.setId(UUID.randomUUID().toString());
            testResourceMapper.insertSelective(testResource);
        } else {
            testResourceMapper.updateByPrimaryKeySelective(testResource);
        }
    }
}
