package com.nov21th.curry.registry.impl;

import com.nov21th.curry.registry.ServiceRegistry;
import com.nov21th.curry.registry.constant.Constants;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 使用ZooKeeper实现的服务发现
 *
 * @author 郭永辉
 * @since 1.0 2017/4/3.
 */
public class ZooKeeperServiceRegistry implements ServiceRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ZooKeeperServiceRegistry.class);

    /**
     * ZooKeeper客户端实例
     * <p>
     * 用于连接ZooKeeper服务器并根据要注册的服务创建相应节点
     * <p>
     * 注册完服务后不能关闭该ZooKeeper客户端的连接，这是由于要进行心跳检测以确定提供服务的服务器是否掉线
     */
    private final ZkClient zkClient;

    /**
     * 所有服务在ZooKeeper下的根节点
     * <p>
     * 例如，若根节点为/com.nov21th，则所有服务都托管在ZooKeeper服务器的/com.nov21th/路径下
     */
    private final String serviceRoot;

    public ZooKeeperServiceRegistry(String zkAddress, String serviceRoot) {
        this(zkAddress, serviceRoot, Constants.DEFAULT_ZK_SESSION_TIMEOUT, Constants.DEFAULT_ZK_CONNECTION_TIMEOUT);
    }

    public ZooKeeperServiceRegistry(String zkAddress, String serviceRoot, int zkSessionTimeout, int zkConnectionTimeout) {
        if (zkAddress == null || "".equals(zkAddress)) {
            throw new RuntimeException("无效的ZooKeeper地址");
        }
        if (serviceRoot == null || "".equals(serviceRoot)) {
            throw new RuntimeException("无效的服务根节点");
        }

        this.serviceRoot = serviceRoot;
        this.zkClient = new ZkClient(zkAddress, zkSessionTimeout, zkConnectionTimeout);

        if (zkAddress.contains(",")) {
            logger.debug("连接到ZooKeeper服务器集群：{}", zkAddress);
        } else {
            logger.debug("连接到ZooKeeper单机服务器：{}", zkAddress);
        }

        if (!zkClient.exists(serviceRoot)) {
            zkClient.createPersistent(serviceRoot);
        }

        logger.debug("服务根节点（持久节点）：{}", serviceRoot);
    }

    public void registerService(String name, String version, String serverAddress) {
        registerService(name + Constants.SERVICE_SEP + version, serverAddress);
    }

    public void registerService(String serviceFullname, String serverAddress) {
        StringBuilder sb = new StringBuilder();

        sb.append(serviceRoot);
        sb.append('/');
        sb.append(serviceFullname);

        String servicePath = sb.toString();
        if (!zkClient.exists(servicePath)) {
            zkClient.createPersistent(servicePath);
        }

        logger.debug("注册服务路径（持久节点）：{}", servicePath);

        sb.append("/address-");

        //注册包含服务地址的临时节点，ZooKeeper客户端断线后该节点会自动被ZooKeeper服务器删除
        String serviceNode = zkClient.createEphemeralSequential(sb.toString(), serverAddress);

        logger.debug("注册服务节点（临时节点）：{}", serviceNode);
    }
}
