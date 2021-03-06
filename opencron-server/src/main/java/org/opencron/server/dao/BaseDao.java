/**
 * Copyright (c) 2015 The Opencron Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.opencron.server.dao;

import org.opencron.common.util.CommonUtils;
import org.opencron.common.util.ReflectUtils;
import org.opencron.common.util.collection.ParamsMap;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.opencron.common.util.CommonUtils.toInt;
import static org.opencron.common.util.CommonUtils.toLong;


@SuppressWarnings("unchecked")
@Transactional(readOnly = true)
public class BaseDao<T, PK extends Serializable> extends HibernateDao {


    /**
     * 当前实体对应的泛型类
     */
    protected Class<T> entityClass = null;

    public BaseDao() {
        entityClass = (Class<T>) ReflectUtils.getGenericType(this.getClass());
    }

    /**
     * 获取实体
     *
     * @param id
     * @return
     */
    public T get(final PK id) {
        return this.get(entityClass, id);
    }


    /**
     * 获取全部实体列表
     *
     * @return
     */
    public List<T> getAll() {
        return this.getAll(entityClass);
    }


    @SuppressWarnings({"hiding"})
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public <T> T merge(T entity) {
        return super.merge(entity);
    }


    @SuppressWarnings({"hiding"})
    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public <T> T save(T entity) {
        return super.save(entity);
    }


    /**
     * 删除实体
     *
     * @param id
     */
    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void delete(final PK id) {
        this.delete(entityClass, id);
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = false)
    public void delete(Object entity) {
        super.delete(entity);
    }

    /**
     * 根据sql获取总数
     *
     * @param sql 需要保证sql为查询总数的语句
     * @return
     */
    public Integer sqlCount(String sql, Object... params) {
        return sqlIntUniqueResult( preparedCount(sql),params);
    }

    public Integer hqlCount(String hql,Object...params) {
        if (hql.toLowerCase().startsWith("from")) {
            hql = "select count(1) " + hql;
        }else {
            hql = "select count(1) " + hql.substring(hql.toLowerCase().indexOf("from"));
        }
        hql = hql.replaceAll("\\s+"," ");

        if (hql.toLowerCase().contains("group by")) {
            List list = createQuery(hql,params).list();
            if (CommonUtils.isEmpty(list)) return 0;
            return list.size();
        }else {
            return hqlIntUniqueResult(hql,params);
        }
    }

    public Integer sqlIntUniqueResult(String sql, Object... params) {
        Object result = createSQLQuery(sql,params).uniqueResult();
        return result==null?null:toInt(result);
    }

    private static String preparedCount(String sql) {
        return  String.format("select count(1) from ( %s ) as t ",sql);
    }

    public Long hqlLongUniqueResult(String hql, Object... params) {
        Object result = createQuery(hql,params).uniqueResult();
        return result==null?null:toLong(result);
    }

    public Integer hqlIntUniqueResult(String hql, Object... params) {
        Object result = createQuery(hql,params).uniqueResult();
        return result==null?null:toInt(result);
    }
}
