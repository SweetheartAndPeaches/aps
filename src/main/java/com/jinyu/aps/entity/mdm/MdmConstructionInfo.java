package com.jinyu.aps.entity.mdm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jinyu.aps.entity.base.ApsBaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 投产胎胚施工信息对象
 * 对应表：T_MDM_CONSTRUCTION_INFO
 *
 * @author APS Team
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("T_MDM_CONSTRUCTION_INFO")
@Schema(description = "投产胎胚施工信息对象")
public class MdmConstructionInfo extends ApsBaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 物料编码
     */
    @Schema(description = "物料编码")
    @TableField("MATERIAL_CODE")
    private String materialCode;

    /**
     * MES物料编码
     */
    @Schema(description = "MES物料编码")
    @TableField("MES_MATERIAL_CODE")
    private String mesMaterialCode;

    /**
     * 规格代号
     */
    @Schema(description = "规格代号")
    @TableField("SPEC_CODE")
    private String specCode;

    /**
     * 胎胚代码(代码)施工号
     */
    @Schema(description = "胎胚代码(代码)施工号")
    @TableField("CONSTRUCTION_CODE")
    private String constructionCode;

    /**
     * BOM版本(存储版本号)
     */
    @Schema(description = "BOM版本")
    @TableField("CONSTRUCTION_VERSION")
    private String constructionVersion;

    /**
     * 成型法
     */
    @Schema(description = "成型法")
    @TableField("MOULD_METHOD")
    private String mouldMethod;

    /**
     * 鼓类型
     */
    @Schema(description = "鼓类型")
    @TableField("BUILDING_DRUM_TYPE")
    private String buildingDrumType;

    /**
     * 寸口
     */
    @Schema(description = "寸口")
    @TableField("PRO_SIZE")
    private BigDecimal proSize;

    /**
     * 花纹
     */
    @Schema(description = "花纹")
    @TableField("PATTERN")
    private String pattern;

    /**
     * 规格
     */
    @Schema(description = "规格")
    @TableField("SPECIFICATIONS")
    private String specifications;

    /**
     * 机头宽度
     */
    @Schema(description = "机头宽度")
    @TableField("HEAD_WIDTH")
    private BigDecimal headWidth;

    /**
     * 扣圈盘直径
     */
    @Schema(description = "扣圈盘直径")
    @TableField("BUCKLE_PLAGE_DIAMETER")
    private BigDecimal bucklePlageDiameter;

    /**
     * 断面宽
     */
    @Schema(description = "断面宽")
    @TableField("SECTION_WIDTH")
    private Integer sectionWidth;

    /**
     * 贴合鼓周长
     */
    @Schema(description = "贴合鼓周长")
    @TableField("FIT_DRUM_PERIMETER")
    private BigDecimal fitDrumPerimeter;

    /**
     * 卡盘直径
     */
    @Schema(description = "卡盘直径")
    @TableField("CHUCK_DIAMETER")
    private BigDecimal chuckDiameter;

    /**
     * 拉伸宽度
     */
    @Schema(description = "拉伸宽度")
    @TableField("STRETCH_WIDTH")
    private BigDecimal stretchWidth;

    /**
     * 定性宽度
     */
    @Schema(description = "定性宽度")
    @TableField("QUALITATIVE_WIDTH")
    private BigDecimal qualitativeWidth;

    /**
     * 胎胚周长
     */
    @Schema(description = "胎胚周长")
    @TableField("EMBRYO_CIRCLE")
    private BigDecimal embryoCircle;

    // ========== 胎体布信息 ==========

    /**
     * 1#胎体布代号
     */
    @Schema(description = "1#胎体布代号")
    @TableField("TIRE_FABRIC_CODE1")
    private String tireFabricCode1;

    /**
     * 1#胎体布BOM版本
     */
    @Schema(description = "1#胎体布BOM版本")
    @TableField("TIRE_FABRIC1_VERSION")
    private String tireFabric1Version;

    /**
     * 1#胎体布工艺
     */
    @Schema(description = "1#胎体布工艺")
    @TableField("TIRE_FABRIC_CRAFT1")
    private String tireFabricCraft1;

    /**
     * 2#胎体布代号
     */
    @Schema(description = "2#胎体布代号")
    @TableField("TIRE_FABRIC_CODE2")
    private String tireFabricCode2;

    /**
     * 2#胎体布BOM版本
     */
    @Schema(description = "2#胎体布BOM版本")
    @TableField("TIRE_FABRIC2_VERSION")
    private String tireFabric2Version;

    /**
     * 2#胎体布工艺
     */
    @Schema(description = "2#胎体布工艺")
    @TableField("TIRE_FABRIC_CRAFT2")
    private String tireFabricCraft2;

    /**
     * 3#胎体布代号
     */
    @Schema(description = "3#胎体布代号")
    @TableField("TIRE_FABRIC_CODE3")
    private String tireFabricCode3;

    /**
     * 3#胎体布BOM版本
     */
    @Schema(description = "3#胎体布BOM版本")
    @TableField("TIRE_FABRIC3_VERSION")
    private String tireFabric3Version;

    /**
     * 3#胎体布工艺
     */
    @Schema(description = "3#胎体布工艺")
    @TableField("TIRE_FABRIC_CRAFT3")
    private String tireFabricCraft3;

    // ========== 帘线信息 ==========

    /**
     * 原线代码
     */
    @Schema(description = "原线代码")
    @TableField("ORIGINAL_LINE_CODE")
    private String originalLineCode;

    /**
     * 帘线规格
     */
    @Schema(description = "帘线规格")
    @TableField("CORD_SPEC")
    private String cordSpec;

    /**
     * 帘布大卷SAP—BOM版本
     */
    @Schema(description = "帘布大卷SAP—BOM版本")
    @TableField("CORD_VERSION")
    private String cordVersion;

    /**
     * 补强/封口胶
     */
    @Schema(description = "补强/封口胶")
    @TableField("REINFORCE_SEAL_GLUE")
    private String reinforceSealGlue;

    // ========== 内衬信息 ==========

    /**
     * 内衬胶料
     */
    @Schema(description = "内衬胶料")
    @TableField("INSIDE_RUBBER")
    private String insideRubber;

    /**
     * 内衬代号
     */
    @Schema(description = "内衬代号")
    @TableField("INSIDE_CODE")
    private String insideCode;

    /**
     * 内衬—BOM版本
     */
    @Schema(description = "内衬—BOM版本")
    @TableField("INSIDE_VERSION")
    private String insideVersion;

    /**
     * 内衬工艺
     */
    @Schema(description = "内衬工艺")
    @TableField("INSIDE_CRAFT")
    private BigDecimal insideCraft;

    // ========== 胎侧信息 ==========

    /**
     * 胎侧代号
     */
    @Schema(description = "胎侧代号")
    @TableField("SIDEWALL_CODE")
    private String sidewallCode;

    /**
     * 垫胶
     */
    @Schema(description = "垫胶")
    @TableField("PADDING_CODE")
    private String paddingCode;

    /**
     * 胶芯
     */
    @Schema(description = "胶芯")
    @TableField("RUBBER_CORE_CODE")
    private String rubberCoreCode;

    /**
     * 零度
     */
    @Schema(description = "零度")
    @TableField("ZERO_BELT_CODE")
    private String zeroBeltCode;

    /**
     * 型胶
     */
    @Schema(description = "型胶")
    @TableField("TYPE_ADHESIVE_CODE")
    private String typeAdhesiveCode;

    /**
     * 包布
     */
    @Schema(description = "包布")
    @TableField("CLOTH_WRAPPING_CODE")
    private String clothWrappingCode;

    /**
     * 胎体
     */
    @Schema(description = "胎体")
    @TableField("TIRE_BODY_CODE")
    private String tireBodyCode;

    /**
     * 胎侧—BOM版本
     */
    @Schema(description = "胎侧—BOM版本")
    @TableField("SIDEWALL_VERSION")
    private String sidewallVersion;

    /**
     * 胎侧工艺
     */
    @Schema(description = "胎侧工艺")
    @TableField("SIDEWALL_CRAFT")
    private BigDecimal sidewallCraft;

    /**
     * 胎侧口型
     */
    @Schema(description = "胎侧口型")
    @TableField("SIDEWALL_MOUTH_PLATE")
    private String sidewallMouthPlate;

    /**
     * 胎侧居中
     */
    @Schema(description = "胎侧居中")
    @TableField("SIDEWALL_CENTER")
    private String sidewallCenter;

    /**
     * 胎侧长度
     */
    @Schema(description = "胎侧长度")
    @TableField("SIDEWALL_LENGTH")
    private BigDecimal sidewallLength;

    /**
     * 胎侧胶料
     */
    @Schema(description = "胎侧胶料")
    @TableField("SIDEWALL_RUBBER")
    private String sidewallRubber;

    /**
     * 胎侧胶重量
     */
    @Schema(description = "胎侧胶重量")
    @TableField("SIDEWALL_WEIGHT")
    private String sidewallWeight;

    /**
     * 胎侧耐磨胶重量
     */
    @Schema(description = "胎侧耐磨胶重量")
    @TableField("SIDEWALL_WEARP_RUBBER_WEIGHT")
    private String sidewallWearpRubberWeight;

    // ========== 支撑胶信息 ==========

    /**
     * 支撑胶代号
     */
    @Schema(description = "支撑胶代号")
    @TableField("SUPPORT_CODE")
    private String supportCode;

    /**
     * 支撑胶料
     */
    @Schema(description = "支撑胶料")
    @TableField("SUPPORT_RUBBER_CODE")
    private String supportRubberCode;

    /**
     * 支撑胶长度
     */
    @Schema(description = "支撑胶长度")
    @TableField("SUPPORT_LENGTH")
    private BigDecimal supportLength;

    // ========== 钢丝圈信息 ==========

    /**
     * 钢丝圈代码
     */
    @Schema(description = "钢丝圈代码")
    @TableField("BEAD_CODE")
    private String beadCode;

    /**
     * 钢丝圈—BOM版本
     */
    @Schema(description = "钢丝圈—BOM版本")
    @TableField("BEAD_VERSION")
    private String beadVersion;

    /**
     * 钢丝圈排列
     */
    @Schema(description = "钢丝圈排列")
    @TableField("BEAD_ARRANGE")
    private String beadArrange;

    /**
     * 钢丝圈类型
     */
    @Schema(description = "钢丝圈类型")
    @TableField("BEAD_TYPE")
    private String beadType;

    // ========== 胎圈信息 ==========

    /**
     * 胎圈代码
     */
    @Schema(description = "胎圈代码")
    @TableField("TIRE_RING_CODE")
    private String tireRingCode;

    /**
     * 胎圈—BOM版本
     */
    @Schema(description = "胎圈—BOM版本")
    @TableField("TIRE_RING_VERSION")
    private String tireRingVersion;

    /**
     * 三角胶代码
     */
    @Schema(description = "三角胶代码")
    @TableField("APEX_CODE")
    private String apexCode;

    /**
     * 六边形圈胶料
     */
    @Schema(description = "六边形圈胶料")
    @TableField("HEXAGON_RUBBER_CODE")
    private String hexagonRubberCode;

    /**
     * 六边形口型
     */
    @Schema(description = "六边形口型")
    @TableField("HEXAGON_MOUTH_PLATE")
    private String hexagonMouthPlate;

    /**
     * 六边形圈尺寸
     */
    @Schema(description = "六边形圈尺寸")
    @TableField("HEXAGON_RUBBER_DIMENSION")
    private String hexagonRubberDimension;

    /**
     * 三角胶重量
     */
    @Schema(description = "三角胶重量")
    @TableField("APEX_WEIGHT")
    private BigDecimal apexWeight;

    // ========== 钢带信息 ==========

    /**
     * 1#钢带代号
     */
    @Schema(description = "1#钢带代号")
    @TableField("BELT_CODE1")
    private String beltCode1;

    /**
     * 1#钢带BOM版本
     */
    @Schema(description = "1#钢带BOM版本")
    @TableField("BELT1_VERSION")
    private String belt1Version;

    /**
     * 1#钢带工艺
     */
    @Schema(description = "1#钢带工艺")
    @TableField("BELT_CRAFT1")
    private BigDecimal beltCraft1;

    /**
     * 1#钢带边胶
     */
    @Schema(description = "1#钢带边胶")
    @TableField("BELT_SIDE_RUBBER1")
    private String beltSideRubber1;

    /**
     * 1#钢带胶料
     */
    @Schema(description = "1#钢带胶料")
    @TableField("BELT_RUBBER1")
    private String beltRubber1;

    /**
     * 2#钢带代号
     */
    @Schema(description = "2#钢带代号")
    @TableField("BELT_CODE2")
    private String beltCode2;

    /**
     * 3#钢带代号
     */
    @Schema(description = "3#钢带代号")
    @TableField("BELT_CODE3")
    private String beltCode3;

    /**
     * 4#钢带代号
     */
    @Schema(description = "4#钢带代号")
    @TableField("BELT_CODE4")
    private String beltCode4;

    /**
     * 左加强层
     */
    @Schema(description = "左加强层")
    @TableField("BELT_CODE_LEFT_CODE")
    private String beltCodeLeftCode;

    /**
     * 右加强层
     */
    @Schema(description = "右加强层")
    @TableField("BELT_CODE_RIGHT_CODE")
    private String beltCodeRightCode;

    /**
     * 2#钢带BOM版本
     */
    @Schema(description = "2#钢带BOM版本")
    @TableField("BELT2_VERSION")
    private String belt2Version;

    /**
     * 2#钢带工艺
     */
    @Schema(description = "2#钢带工艺")
    @TableField("BELT_CRAFT2")
    private BigDecimal beltCraft2;

    /**
     * 2#钢带边胶
     */
    @Schema(description = "2#钢带边胶")
    @TableField("BELT_SIDE_RUBBER2")
    private String beltSideRubber2;

    /**
     * 2#钢带胶料
     */
    @Schema(description = "2#钢带胶料")
    @TableField("BELT_RUBBER2")
    private String beltRubber2;

    /**
     * 钢带裁断角度
     */
    @Schema(description = "钢带裁断角度")
    @TableField("BELT_CUTTING_ANGLE")
    private String beltCuttingAngle;

    /**
     * 钢带规格
     */
    @Schema(description = "钢带规格")
    @TableField("ARTICLE_CROWN_SPEC")
    private String articleCrownSpec;

    /**
     * 钢压大卷BOM版本
     */
    @Schema(description = "钢压大卷BOM版本")
    @TableField("ARTICLE_CROWN_VERSION")
    private String articleCrownVersion;

    /**
     * 冠带条代号
     */
    @Schema(description = "冠带条代号")
    @TableField("ARTICLE_CROWN_CODE")
    private String articleCrownCode;

    // ========== 胎面信息 ==========

    /**
     * 胎面代号
     */
    @Schema(description = "胎面代号")
    @TableField("TREAD_CODE")
    private String treadCode;

    /**
     * 胎面BOM版本
     */
    @Schema(description = "胎面BOM版本")
    @TableField("TREAD_VERSION")
    private String treadVersion;

    /**
     * 胎面宽
     */
    @Schema(description = "胎面宽")
    @TableField("TREAD_SHOULDER_WIDTH")
    private BigDecimal treadShoulderWidth;

    /**
     * 胎面肩宽
     */
    @Schema(description = "胎面肩宽")
    @TableField("TREAD_SHOULDER_JWIDTH")
    private BigDecimal treadShoulderJwidth;

    /**
     * 胎面长
     */
    @Schema(description = "胎面长")
    @TableField("TREAD_SHOULDER_LENGTH")
    private BigDecimal treadShoulderLength;

    /**
     * 胎面胶种
     */
    @Schema(description = "胎面胶种")
    @TableField("TREAD_RUBBER_CATEGORY")
    private String treadRubberCategory;

    /**
     * 重量kg/条（上胎冠）
     */
    @Schema(description = "重量kg/条（上胎冠）")
    @TableField("TIRE_CROWN_UP_WIDTH_WEIGHT")
    private BigDecimal tireCrownUpWidthWeight;

    /**
     * 重量kg/条（下胎冠）
     */
    @Schema(description = "重量kg/条（下胎冠）")
    @TableField("TIRE_CROWN_DOWN_WIDTH_WEIGHT")
    private BigDecimal tireCrownDownWidthWeight;

    /**
     * 重量kg/条（胎翼）
     */
    @Schema(description = "重量kg/条（胎翼）")
    @TableField("TIRE_WING_WIDTH_WEIGHT")
    private BigDecimal tireWingWidthWeight;

    /**
     * 重量kg/条（底胶）
     */
    @Schema(description = "重量kg/条（底胶）")
    @TableField("PRIMER_WEIGHT")
    private BigDecimal primerWeight;

    /**
     * 重量kg/条（导电胶）
     */
    @Schema(description = "重量kg/条（导电胶）")
    @TableField("CONDUCTING_RESIN_WEIGHT")
    private BigDecimal conductingResinWeight;

    /**
     * 胎面口型板
     */
    @Schema(description = "胎面口型板")
    @TableField("TREAD_MOUTH_PLATE")
    private String treadMouthPlate;

    // ========== 硫化相关 ==========

    /**
     * 合模压力PA
     */
    @Schema(description = "合模压力PA")
    @TableField("MOULD_CLAMPING_PRESSURE")
    private BigDecimal mouldClampingPressure;

    /**
     * 机械硫化时间(秒)
     */
    @Schema(description = "机械硫化时间(秒)")
    @TableField("CURING_TIME")
    private Integer curingTime;

    /**
     * 液压硫化时间(秒)
     */
    @Schema(description = "液压硫化时间(秒)")
    @TableField("HY_PRESSURE_CURING_TIME")
    private Integer hydraulicPressureCuringTime;

    /**
     * 模具型腔
     */
    @Schema(description = "模具型腔")
    @TableField("MOLD_CAVITY")
    private String moldCavity;

    /**
     * 生产阶段（0：投产阶段；1试制阶段）
     */
    @Schema(description = "生产阶段（0：投产阶段；1试制阶段）")
    @TableField("PRODUCTION_STAGE")
    private String productionStage;
}
