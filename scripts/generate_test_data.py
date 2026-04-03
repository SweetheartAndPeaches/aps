#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
根据Excel文件生成APS测试数据
生成2026-09-01至2026-09-05共5天的硫化排程结果数据
"""
import pandas as pd
import numpy as np
from datetime import datetime, timedelta

file_path = 'assets/TBR成型计划硫化计划2024年07月07日(4).xls'

# 设置目标日期范围
START_DATE = datetime(2026, 9, 1)
END_DATE = datetime(2026, 9, 5)
FACTORY_CODE = '116'

# 读取数据
df_lh = pd.read_excel(file_path, sheet_name='硫化计划', header=None)
df_month = pd.read_excel(file_path, sheet_name='月计划', header=None)
df_cx = pd.read_excel(file_path, sheet_name='成型计划', header=None)

# 生成SQL语句
sql_statements = []

# 1. 成型机台数据 (基于Excel中的机台模式)
sql_statements.append("-- ========== 成型机台数据 ==========")
forming_machines = ['GM01', 'GM02', 'GM03', 'GM04', 'GM05', 'GM06', 'GM07', 'GM08']
for i, machine in enumerate(forming_machines, 1):
    sql = f"""INSERT INTO T_MDM_MOLDING_MACHINE (CX_MACHINE_CODE, CX_MACHINE_BRAND_CODE, CX_MACHINE_TYPE_CODE, ROLL_OVER_TYPE, IS_ZERO_RACK, LH_MACHINE_MAX_QTY, MAX_DAY_CAPACITY, LINE_NUMBER, IS_ACTIVE) VALUES ('{machine}', '软控', '三鼓', 'A型', 1, 4, 120, {(i-1)//3 + 1}, 1);"""
    sql_statements.append(sql)

# 2. 硫化排程结果数据 (生成5天数据)
sql_statements.append("\n-- ========== 硫化排程结果数据 ==========")
vulcanizing_machines = []
for i in range(7, 80):
    row = df_lh.iloc[i]
    machine = row[4]
    if pd.notna(machine) and isinstance(machine, str) and machine not in vulcanizing_machines:
        vulcanizing_machines.append(machine)
        # 为每个机台生成5天的排程数据
        current_date = START_DATE
        while current_date <= END_DATE:
            date_str = current_date.strftime('%Y-%m-%d')
            daily_qty = int(row[9]) if pd.notna(row[9]) else 0
            # 按照班次比例分配：夜班:早班:中班 = 1:2:1
            class1 = daily_qty // 4 if daily_qty >= 4 else (1 if daily_qty >= 1 else 0)
            class3 = daily_qty // 4 if daily_qty >= 4 else (1 if daily_qty >= 2 else 0)
            class2 = daily_qty - class1 - class3
            embryo_stock = int(row[8]) if pd.notna(row[8]) else 0
            
            sql = f"""INSERT INTO T_LH_SCHEDULE_RESULT (FACTORY_CODE, LH_MACHINE_CODE, PRODUCT_CODE, SPEC_DESC, SPEC_CODE, DAILY_PLAN_QTY, SCHEDULE_DATE, CLASS1_PLAN_QTY, CLASS2_PLAN_QTY, CLASS3_PLAN_QTY, EMBRYO_STOCK, PRODUCTION_STATUS) VALUES ('{FACTORY_CODE}', '{machine}', '{row[5]}', '{row[6]}', '{row[6].split()[0] if pd.notna(row[6]) else ''}', {daily_qty}, '{date_str}', {class1}, {class2}, {class3}, {embryo_stock}, '0');"""
            sql_statements.append(sql)
            current_date += timedelta(days=1)

# 3. 物料主数据
sql_statements.append("\n-- ========== 物料主数据 ==========")
materials = {}
for i in range(7, 50):
    row = df_lh.iloc[i]
    material_code = row[5]
    material_desc = row[6]
    if pd.notna(material_code) and material_code not in materials:
        materials[material_code] = material_desc
        # 解析规格
        desc_parts = str(material_desc).split() if pd.notna(material_desc) else ['', '']
        spec = desc_parts[0] if len(desc_parts) > 0 else ''
        pattern = desc_parts[1] if len(desc_parts) > 1 else ''
        # 根据建表语句字段：MATERIAL_CODE, MATERIAL_NAME, SPECIFICATION, STRUCTURE_NAME, MAIN_PATTERN, PATTERN, EMBRYO_CODE, SPEC_DESC, LH_TIME
        sql = f"""INSERT INTO T_MDM_MATERIAL_INFO (MATERIAL_CODE, MATERIAL_NAME, SPECIFICATION, STRUCTURE_NAME, MAIN_PATTERN, PATTERN, EMBRYO_CODE, SPEC_DESC, LH_TIME) VALUES ('{material_code}', '{material_desc}', '{spec}', '{spec}', '{pattern}', '{pattern}', '{material_code}', '{material_desc}', 13);"""
        sql_statements.append(sql)

# 4. 胎胚库存数据
sql_statements.append("\n-- ========== 胎胚库存数据 ==========")
for i in range(7, 50):
    row = df_lh.iloc[i]
    material_code = row[5]
    embryo_stock = row[8] if pd.notna(row[8]) else 0
    if pd.notna(material_code):
        sql = f"""INSERT INTO T_CX_STOCK (STOCK_DATE, EMBRYO_CODE, STOCK_NUM, OVER_TIME_STOCK, MODIFY_NUM, BAD_NUM, IS_ENDING_SKU) VALUES ('{START_DATE.strftime('%Y-%m-%d')}', '{material_code}', {int(embryo_stock)}, 0, 0, 0, 0);"""
        sql_statements.append(sql)

# 5. 月计划数据
sql_statements.append("\n-- ========== 月计划数据 ==========")
for i in range(1, len(df_month)):
    row = df_month.iloc[i]
    material_code = row[0]
    material_desc = row[1]
    total_qty = row[2]
    rubber_type = row[3]
    if pd.notna(material_code):
        sql = f"""INSERT INTO T_MP_MONTH_PLAN_PROD_FINAL (MATERIAL_CODE, MATERIAL_DESC, PLAN_QTY, RUBBER_TYPE, PLAN_MONTH, PLAN_YEAR) VALUES ('{material_code}', '{material_desc}', {total_qty}, '{rubber_type}', '{START_DATE.strftime('%Y-%m')}', {START_DATE.year});"""
        sql_statements.append(sql)

# 6. 结构硫化配比数据
sql_statements.append("\n-- ========== 结构硫化配比数据 ==========")
structures = set()
for i in range(7, 50):
    row = df_lh.iloc[i]
    material_desc = row[6]
    if pd.notna(material_desc):
        spec = str(material_desc).split()[0] if len(str(material_desc).split()) > 0 else ''
        if spec and spec not in structures:
            structures.add(spec)
            sql = f"""INSERT INTO T_MDM_STRUCTURE_LH_RATIO (STRUCTURE_NAME, CX_MACHINE_TYPE_CODE, LH_MACHINE_MAX_QTY, MAX_EMBRYO_QTY, IS_ACTIVE) VALUES ('{spec}', '三鼓', 4, 4, 1);"""
            sql_statements.append(sql)

# 输出所有SQL
print('\n'.join(sql_statements))
