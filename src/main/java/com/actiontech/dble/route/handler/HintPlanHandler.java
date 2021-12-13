/*
 * Copyright (C) 2016-2021 ActionTech.
 * License: http://www.gnu.org/licenses/gpl.html GPL version 2 or higher.
 */

package com.actiontech.dble.route.handler;


import com.actiontech.dble.plan.optimizer.HintPlanInfo;
import com.actiontech.dble.plan.optimizer.HintPlanNodeGroup;
import com.actiontech.dble.route.RouteResultset;
import com.actiontech.dble.route.parser.util.DruidUtil;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.google.common.collect.Lists;

import java.sql.SQLSyntaxErrorException;
import java.util.List;

/**
 * sql hint: dble:plan= (a,b,c)&b&(c,d)<br/>
 *
 * @author collapsar
 */
public final class HintPlanHandler {

    private HintPlanHandler() {
    }

    public static RouteResultset route(String hintSQL, int sqlType, String realSql) throws SQLSyntaxErrorException {
        String[] attr = hintSQL.split("\\$");
        List<HintPlanNodeGroup> nodes = Lists.newLinkedList();
        String hintTable = attr[0];
        String[] tables = hintTable.split("&");
        for (String table : tables) {
            if (table.contains(",")) {
                // ER
                table = table.replaceAll("[()\\s]", "");
                String[] ers = table.split(",");
                nodes.add(HintPlanNodeGroup.of(HintPlanNodeGroup.Type.ER, ers));
            } else if (table.contains("|")) {
                table = table.replaceAll("[()\\s]", "");
                String[] ers = table.split("\\|");
                nodes.add(HintPlanNodeGroup.of(HintPlanNodeGroup.Type.OR, ers));
            } else {
                table = table.trim();
                nodes.add(HintPlanNodeGroup.of(HintPlanNodeGroup.Type.AND, table));
            }
        }

        HintPlanInfo planInfo = new HintPlanInfo(nodes);
        if (attr.length > 1) {
            for (int i = 1; i < attr.length; i++) {
                if (attr[i].equalsIgnoreCase("left2Inner")) {
                    planInfo.setLft2inner(true);
                } else if (attr[i].equalsIgnoreCase("in2join")) {
                    planInfo.setIn2join(true);
                }
            }
        }
        RouteResultset rrs = new RouteResultset(realSql, sqlType);
        rrs.setHintPlanInfo(planInfo);
        SQLStatement statement = DruidUtil.parseSQL(realSql);
        rrs.setNeedOptimizer(true);
        rrs.setSqlStatement(statement);
        return rrs;
    }

}
