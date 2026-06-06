-- MySQL dump 10.13  Distrib 9.0.0, for Win64 (x86_64)
--
-- Host: localhost    Database: weib
-- ------------------------------------------------------
-- Server version	9.0.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `applications`
--

DROP TABLE IF EXISTS `applications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `boss_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `job_id` bigint NOT NULL,
  `resume_id` bigint NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `applications`
--

LOCK TABLES `applications` WRITE;
/*!40000 ALTER TABLE `applications` DISABLE KEYS */;
INSERT INTO `applications` VALUES (1,NULL,'2026-05-16 18:34:57.000000',1,1,'pending','2026-05-16 18:34:57.000000',7),(2,NULL,'2026-05-16 18:34:57.000000',11,1,'interview','2026-05-16 18:34:57.000000',7),(3,NULL,'2026-05-16 18:34:57.000000',21,1,'pending','2026-05-16 18:34:57.000000',7),(4,NULL,'2026-05-16 18:34:57.000000',2,2,'interview','2026-05-16 18:34:57.000000',8),(5,NULL,'2026-05-16 18:34:57.000000',12,2,'pending','2026-05-16 18:34:57.000000',8),(6,NULL,'2026-05-16 18:34:57.000000',35,2,'pending','2026-05-16 18:34:57.000000',8),(7,NULL,'2026-05-16 18:34:57.000000',8,3,'accepted','2026-05-16 18:34:57.000000',9),(8,NULL,'2026-05-16 18:34:57.000000',16,3,'pending','2026-05-16 18:34:57.000000',9),(9,NULL,'2026-05-16 18:34:57.000000',41,4,'interview','2026-05-16 18:34:57.000000',10),(10,NULL,'2026-05-16 18:34:57.000000',46,4,'pending','2026-05-16 18:34:57.000000',10),(11,NULL,'2026-05-16 18:34:57.000000',12,5,'accepted','2026-05-16 18:34:57.000000',11),(12,NULL,'2026-05-16 18:34:57.000000',19,5,'pending','2026-05-16 18:34:57.000000',11),(13,NULL,'2026-05-16 18:34:57.000000',26,6,'interview','2026-05-16 18:34:57.000000',12),(14,NULL,'2026-05-16 18:34:57.000000',22,6,'pending','2026-05-16 18:34:57.000000',12),(15,NULL,'2026-05-16 18:34:57.000000',3,7,'pending','2026-05-16 18:34:57.000000',13),(16,NULL,'2026-05-16 18:34:57.000000',27,7,'pending','2026-05-16 18:34:57.000000',13),(17,NULL,'2026-05-16 18:34:57.000000',44,8,'accepted','2026-05-16 18:34:57.000000',14),(18,NULL,'2026-05-16 18:34:57.000000',41,8,'rejected','2026-05-16 18:34:57.000000',14),(19,NULL,'2026-05-16 18:34:57.000000',7,9,'pending','2026-05-16 18:34:57.000000',15),(20,NULL,'2026-05-16 18:34:57.000000',24,9,'interview','2026-05-16 18:34:57.000000',15),(21,NULL,'2026-05-16 18:34:57.000000',10,10,'pending','2026-05-16 18:34:57.000000',16),(22,NULL,'2026-05-16 18:34:57.000000',30,10,'interview','2026-05-16 18:34:57.000000',16),(23,NULL,'2026-05-16 18:34:57.000000',24,11,'accepted','2026-05-16 18:34:57.000000',17),(24,NULL,'2026-05-16 18:34:57.000000',40,12,'pending','2026-05-16 18:34:57.000000',18),(25,NULL,'2026-05-16 18:34:57.000000',11,13,'interview','2026-05-16 18:34:57.000000',19),(26,NULL,'2026-05-22 10:50:03.820170',5,2,'pending','2026-05-22 10:50:03.820170',8);
/*!40000 ALTER TABLE `applications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `companies`
--

DROP TABLE IF EXISTS `companies`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `companies` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `boss_id` bigint NOT NULL,
  `contact_email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contact_phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `industry` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `logo` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scale` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `companies`
--

LOCK TABLES `companies` WRITE;
/*!40000 ALTER TABLE `companies` DISABLE KEYS */;
INSERT INTO `companies` VALUES (1,'北京市海淀区知春路甲48号',1,'zhang@bytedance.com','张总','13800138001','2026-05-16 18:34:57.000000','字节跳动是一家全球化的科技公司，旗下拥有抖音、今日头条、TikTok等知名产品。公司致力于用科技连接人和信息，创造美好生活。我们提供极具竞争力的薪酬福利和广阔的发展空间。','互联网/科技',NULL,'字节跳动科技有限公司','10000人以上','2026-05-16 18:34:57.000000',39.980557,116.337649),(2,'深圳市龙岗区坂田华为基地',2,'li@huawei.com','李总','13800138002','2026-05-16 18:34:57.000000','华为是全球领先的ICT基础设施和智能终端提供商。公司坚持开放合作，持续创新，致力于构建万物互联的智能世界。我们为员工提供全球化工作机会和完善的培养体系。','通信/IT',NULL,'华为技术有限公司','10000人以上','2026-05-16 18:34:57.000000',22.654232,114.062832),(3,'杭州市余杭区文一西路969号',3,'wang@alibaba.com','王总','13800138003','2026-05-16 18:34:57.000000','阿里巴巴集团是全球领先的电子商务和科技公司，业务涵盖电商、云计算、数字媒体、物流等多个领域。我们秉持\"让天下没有难做的生意\"的使命。','互联网/电商',NULL,'阿里巴巴集团','10000人以上','2026-05-16 18:34:57.000000',30.282197,120.025726),(4,'深圳市南山区科技园南区',4,'zhao@weizhong.com','赵总','13800138004','2026-05-16 18:34:57.000000','微众科技是一家专注于金融科技领域的创新企业，致力于用大数据和人工智能技术赋能传统金融机构。核心团队来自腾讯、蚂蚁金服等知名企业，已完成C轮融资。','金融科技',NULL,'深圳微众科技有限公司','500-2000人','2026-05-16 18:34:57.000000',22.540928,113.953175),(5,'上海市静安区南京西路1788号',5,'chen@xingyun.com','陈总','13800138005','2026-05-16 18:34:57.000000','星云互娱是一家专注于数字内容创作的创新型公司，涵盖短视频制作、直播运营、IP孵化等业务。公司氛围年轻活力，重视创意和执行力。','文化传媒',NULL,'上海星云互娱文化有限公司','200-500人','2026-05-16 18:34:57.000000',31.228444,121.453818);
/*!40000 ALTER TABLE `companies` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorite_jobs`
--

DROP TABLE IF EXISTS `favorite_jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorite_jobs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `job_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK7okf4hnen3bg7odf1ntfddoto` (`job_id`,`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorite_jobs`
--

LOCK TABLES `favorite_jobs` WRITE;
/*!40000 ALTER TABLE `favorite_jobs` DISABLE KEYS */;
INSERT INTO `favorite_jobs` VALUES (1,'2026-05-28 22:46:10.760542',35,8);
/*!40000 ALTER TABLE `favorite_jobs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorites`
--

DROP TABLE IF EXISTS `favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `job_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorites`
--

LOCK TABLES `favorites` WRITE;
/*!40000 ALTER TABLE `favorites` DISABLE KEYS */;
/*!40000 ALTER TABLE `favorites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `jobs`
--

DROP TABLE IF EXISTS `jobs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `jobs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `address` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `city` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `company_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `education` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `experience` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requirements` text COLLATE utf8mb4_unicode_ci,
  `salary_max` int DEFAULT NULL,
  `salary_min` int DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tags` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `view_count` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `jobs`
--

LOCK TABLES `jobs` WRITE;
/*!40000 ALTER TABLE `jobs` DISABLE KEYS */;
INSERT INTO `jobs` VALUES (1,'北京市海淀区知春路甲48号','北京',1,'2026-05-16 18:34:57.000000','负责抖音电商后台核心系统的设计与开发；参与高并发分布式系统的架构优化；编写高质量代码，进行代码review；解决系统性能瓶颈和技术难题。\n\n我们提供：\n- 极具竞争力的薪资和股票期权\n- 免费三餐和下午茶\n- 弹性工作制\n- 完善的培训体系和晋升通道','本科','3-5年','1. 计算机相关专业本科及以上学历\n2. 3年以上Java开发经验，熟悉Spring Boot、Spring Cloud\n3. 熟悉MySQL、Redis、Kafka等中间件\n4. 有高并发系统开发经验\n5. 良好的沟通能力和团队协作精神',55000,30000,'active','Java,Spring Boot,分布式,高并发,抖音','高级Java开发工程师','2026-05-28 21:53:12.040983',9),(2,'北京市海淀区知春路甲48号','北京',1,'2026-05-16 18:34:57.000000','负责字节跳动旗下产品的前端开发；与产品、设计、后端紧密协作；持续优化前端性能和用户体验；参与前端基础设施建设和工具链开发。','本科','2-5年','1. 熟悉HTML5、CSS3、JavaScript ES6+\n2. 精通React框架，了解Vue更佳\n3. 熟悉Webpack、Babel等构建工具\n4. 有TypeScript项目经验\n5. 了解Node.js服务端开发',45000,25000,'active','React,TypeScript,前端,Webpack,Node.js','前端开发工程师（React）','2026-05-28 21:56:06.250319',7),(3,'北京市海淀区知春路甲48号','北京',1,'2026-05-16 18:34:57.000000','负责电商产品的需求分析、功能规划和版本迭代；深入用户研究，挖掘用户需求和痛点；撰写PRD文档，推动产品从概念到上线的全过程；跟踪产品数据，持续优化产品体验。','本科','3-5年','1. 3年以上互联网产品经验，有电商经验优先\n2. 优秀的数据分析能力和用户洞察力\n3. 熟练使用Axure、Figma等原型工具\n4. 出色的沟通协调能力和项目管理能力\n5. 对电商行业有深刻理解和热情',50000,28000,'active','产品经理,电商,数据分析,PRD,用户研究','产品经理（电商方向）','2026-05-16 18:34:57.000000',0),(4,'北京市海淀区知春路甲48号','北京',1,'2026-05-16 18:34:57.000000','负责质量保障体系的建设；开发自动化测试框架和工具；参与CI/CD流程建设；进行性能测试和安全测试。','本科','1-3年','1. 计算机相关专业\n2. 熟悉Java/Python至少一种\n3. 了解Selenium、Appium等测试框架\n4. 有持续集成经验\n5. 良好的逻辑分析能力',40000,20000,'active','测试开发,自动化测试,CI/CD,Python,Java','测试开发工程师','2026-05-16 18:34:57.000000',0),(5,'北京市海淀区知春路甲48号','北京',1,'2026-05-16 18:34:57.000000','负责产品的视觉设计和交互体验优化；制定设计规范和组件库；与产品经理和开发团队紧密协作；跟踪设计趋势，持续提升产品设计品质。','本科','2-4年','1. 设计相关专业背景\n2. 精通Figma、Sketch等设计工具\n3. 有完整的移动端设计作品集\n4. 了解前端开发基础知识\n5. 有良好的审美和创造力',40000,22000,'active','UI设计,UX,交互设计,Figma,设计规范','UI/UX设计师','2026-05-28 21:34:08.868991',2),(6,'北京市海淀区知春路甲48号','北京',1,'2026-05-16 18:34:57.000000','负责业务数据的采集、清洗和分析；搭建数据看板和报表系统；通过数据挖掘发现业务增长点；支持产品和运营决策。','本科','1-3年','1. 统计学、数学或计算机相关专业\n2. 熟练使用SQL和Python\n3. 熟悉Tableau、PowerBI等可视化工具\n4. 有AB测试和用户画像经验优先\n5. 良好的数据敏感度和业务理解能力',35000,18000,'active','数据分析,SQL,Python,可视化,用户画像','数据分析师','2026-05-16 18:34:57.000000',0),(7,'北京市海淀区知春路甲48号','北京',1,'2026-05-16 18:34:57.000000','负责线上服务的稳定性和可靠性保障；构建监控和告警体系；优化资源利用率和成本；参与故障处理和复盘。','本科','3-5年','1. 熟悉Linux系统管理和网络原理\n2. 精通Shell/Python脚本编程\n3. 熟悉Docker和Kubernetes\n4. 了解Prometheus、Grafana等监控工具\n5. 有大规模集群运维经验',45000,25000,'active','运维,SRE,Kubernetes,Docker,监控','运维工程师（SRE）','2026-05-16 18:34:57.000000',0),(8,'北京市海淀区知春路甲48号','北京',1,'2026-05-16 18:34:57.000000','负责推荐算法的研发和优化；构建用户画像和内容理解模型；进行离线评估和在线AB实验；跟踪学术界最新进展并落地到业务。','硕士','3-5年','1. 计算机/数学/统计相关专业硕士及以上\n2. 精通Python和C++\n3. 熟悉TensorFlow/PyTorch\n4. 有推荐系统或广告算法经验\n5. 发表过顶会论文优先',65000,35000,'active','算法,推荐系统,机器学习,深度学习,Python','算法工程师（推荐系统）','2026-05-16 18:34:57.000000',0),(9,'北京市海淀区知春路甲48号','北京',1,'2026-05-16 18:34:57.000000','负责跨团队技术项目的整体规划和推进；制定项目排期，协调多方资源；管理项目风险和变更；定期汇报项目进展。','本科','5-10年','1. 计算机相关专业本科及以上\n2. 5年以上软件项目管理经验\n3. 了解敏捷开发流程\n4. 优秀的沟通和协调能力\n5. PMP认证优先',50000,30000,'active','项目管理,敏捷,技术管理,PMP,跨团队协作','技术项目经理','2026-05-16 18:34:57.000000',0),(10,'北京市海淀区知春路甲48号','北京',1,'2026-05-16 18:34:57.000000','负责抖音Android客户端的功能开发和性能优化；与产品、设计团队协作，提升用户体验；参与App架构优化和组件化改造。','本科','2-5年','1. 精通Java/Kotlin开发\n2. 熟悉Android Framework和常见第三方库\n3. 了解性能优化和内存管理\n4. 有音视频开发经验优先\n5. 良好的代码规范和文档习惯',45000,25000,'active','Android,Kotlin,Java,移动开发,性能优化','Android开发工程师','2026-05-16 18:34:57.000000',0),(11,'深圳市龙岗区坂田华为基地','深圳',2,'2026-05-16 18:34:57.000000','负责5G基带信号处理算法的研究与实现；参与物理层算法的设计和仿真验证；与硬件团队协作完成算法落地；跟踪3GPP标准演进。','硕士','3-5年','1. 通信/电子/信号处理相关专业硕士及以上\n2. 精通MATLAB和C/C++\n3. 熟悉OFDM、MIMO等通信技术\n4. 了解3GPP 5G NR标准\n5. 有基带算法开发经验',60000,30000,'active','5G,通信算法,MATLAB,C++,基带','5G通信算法工程师','2026-05-28 22:45:39.933607',1),(12,'深圳市龙岗区坂田华为基地','深圳',2,'2026-05-16 18:34:57.000000','负责嵌入式系统软件开发；参与底层驱动开发和系统优化；编写设计文档和测试用例；解决产品量产中的技术问题。','本科','2-5年','1. 电子/计算机相关专业\n2. 精通C语言，了解汇编\n3. 熟悉Linux内核和驱动开发\n4. 有ARM/MIPS平台经验\n5. 了解RTOS实时操作系统',40000,20000,'active','嵌入式,Linux驱动,C语言,ARM,RTOS','嵌入式软件开发工程师','2026-05-16 18:34:57.000000',0),(13,'深圳市龙岗区坂田华为基地','深圳',2,'2026-05-16 18:34:57.000000','负责华为云核心服务的架构设计；制定技术演进路线和方案评审；解决大规模分布式系统的关键技术难题；指导团队进行技术攻关。','本科','5-10年','1. 8年以上后端开发经验\n2. 精通分布式系统设计\n3. 熟悉Kubernetes和Service Mesh\n4. 有大规模云服务架构经验\n5. 出色的技术领导力',80000,40000,'active','云架构,Kubernetes,分布式系统,华为云,技术领导','云服务架构师','2026-05-16 18:34:57.000000',0),(14,'深圳市龙岗区坂田华为基地','深圳',2,'2026-05-16 18:34:57.000000','参与麒麟芯片的架构设计和RTL实现；进行时序分析和功耗优化；编写验证计划，参与芯片验证；与后端团队协作完成tape-out。','硕士','3-5年','1. 微电子/集成电路相关专业硕士及以上\n2. 熟练使用Verilog/VHDL\n3. 了解DC/PT等EDA工具\n4. 有数字芯片设计经验\n5. 了解先进制程工艺',70000,35000,'active','芯片设计,Verilog,RTL,EDA,麒麟','芯片设计工程师','2026-05-16 18:34:57.000000',0),(15,'深圳市龙岗区坂田华为基地','深圳',2,'2026-05-16 18:34:57.000000','负责公司网络安全防护体系的建设和运营；进行安全漏洞扫描和渗透测试；应急响应和安全事件处理；安全管理制度和规范的制定。','本科','3-5年','1. 信息安全相关专业\n2. 熟悉Web安全和网络安全攻防技术\n3. 熟练使用Burp Suite、Metasploit等工具\n4. 持有CISP/CISSP认证优先\n5. 有安全应急响应经验',50000,25000,'active','网络安全,渗透测试,漏洞扫描,CISP,安全防护','网络安全工程师','2026-05-16 18:34:57.000000',0),(16,'深圳市龙岗区坂田华为基地','深圳',2,'2026-05-16 18:34:57.000000','负责AI算法在终端产品中的落地和优化；进行模型压缩和推理加速；与算法团队协作完成端到端AI解决方案。','本科','2-5年','1. 计算机相关专业\n2. 熟悉Python/C++编程\n3. 了解TensorFlow Lite/ONNX Runtime\n4. 有模型量化和剪枝经验优先\n5. 了解移动端AI部署',45000,25000,'active','AI应用,模型压缩,ONNX,移动端AI,Python','AI应用开发工程师','2026-05-28 22:03:02.867668',1),(17,'深圳市龙岗区坂田华为基地','深圳',2,'2026-05-16 18:34:57.000000','负责全球供应链的规划和优化；管理供应商关系和采购策略；进行库存控制和成本分析；推动供应链数字化转型。','本科','3-5年','1. 供应链/物流管理相关专业\n2. 3年以上供应链管理经验\n3. 熟悉ERP系统（SAP优先）\n4. 优秀的数据分析和谈判能力\n5. 英语可作为工作语言',40000,20000,'active','供应链,SAP,采购管理,库存控制,数字化','供应链管理专家','2026-05-16 18:34:57.000000',0),(18,'深圳市龙岗区坂田华为基地','深圳',2,'2026-05-16 18:34:57.000000','负责产品技术文档的编写和维护；与研发团队协作获取技术资料；翻译和本地化英文技术文档；规范化文档管理流程。','本科','1-3年','1. 通信/计算机/英语相关专业\n2. 出色的中英文读写能力\n3. 熟悉Markdown/DITA等文档工具\n4. 了解技术写作规范\n5. 有技术翻译经验优先',25000,15000,'active','技术文档,技术写作,翻译,Markdown,DITA','技术文档工程师','2026-05-16 18:34:57.000000',0),(19,'深圳市龙岗区坂田华为基地','深圳',2,'2026-05-16 18:34:57.000000','负责通信设备硬件测试方案的制定和执行；使用测试仪器进行射频、基带等测试；编写测试报告，跟踪问题闭环；测试自动化脚本开发。','本科','2-4年','1. 电子/通信相关专业\n2. 熟悉示波器、频谱仪等测试仪器\n3. 了解射频测试原理\n4. 会Python/LabVIEW编程\n5. 有通信设备测试经验',35000,18000,'active','硬件测试,射频,示波器,Python,通信设备','硬件测试工程师','2026-05-16 18:34:57.000000',0),(20,'深圳市龙岗区坂田华为基地','深圳',2,'2026-05-16 18:34:57.000000','负责华为终端产品的品牌建设和营销策略；策划大型品牌活动和发布会；管理品牌预算和ROI分析；与全球市场团队协作。','本科','5-10年','1. 市场营销/广告相关专业\n2. 5年以上品牌营销经验\n3. 有科技产品营销案例\n4. 出色的创意思维和执行力\n5. 英语流利，有国际视野',45000,25000,'active','品牌营销,营销策略,品牌建设,发布会,国际视野','品牌营销经理','2026-05-16 18:34:57.000000',0),(21,'杭州市余杭区文一西路969号','杭州',3,'2026-05-16 18:34:57.000000','负责淘宝核心交易链路的开发和维护；参与双11大促技术保障；进行系统性能优化和架构升级；指导初中级工程师成长。','本科','5-10年','1. 5年以上Java开发经验\n2. 精通分布式系统设计\n3. 有大规模电商系统经验\n4. 熟悉中间件技术（消息队列、缓存等）\n5. 有技术团队管理经验',60000,35000,'active','Java,淘宝,分布式,高并发,双11','资深Java开发工程师','2026-05-16 18:34:57.000000',0),(22,'杭州市余杭区文一西路969号','杭州',3,'2026-05-16 18:34:57.000000','负责大数据平台的建设和维护；开发ETL数据管道和实时计算任务；数据仓库建模和治理；支持业务的数据分析需求。','本科','3-5年','1. 精通SQL和Java/Python\n2. 熟悉Hadoop/Spark/Flink生态\n3. 了解数据仓库建模方法论\n4. 有实时计算经验\n5. 了解数据治理和数据质量',50000,28000,'active','大数据,Spark,Flink,Hadoop,数据仓库','大数据开发工程师','2026-05-16 18:34:57.000000',0),(23,'杭州市余杭区文一西路969号','杭州',3,'2026-05-16 18:34:57.000000','负责支付宝国际支付产品的规划和迭代；调研海外市场支付需求和监管政策；与银行、钱包等合作伙伴对接；推动跨境支付产品的落地。','本科','3-5年','1. 3年以上支付/金融产品经验\n2. 了解跨境支付业务和监管\n3. 优秀的英语沟通能力\n4. 数据分析能力强\n5. 有海外工作经验优先',55000,30000,'active','支付,跨境支付,支付宝,金融产品,国际化','国际支付产品经理','2026-05-16 18:34:57.000000',0),(24,'杭州市余杭区文一西路969号','杭州',3,'2026-05-16 18:34:57.000000','负责自然语言处理领域的前沿研究；在顶级会议和期刊发表论文；将研究成果应用到业务场景中；与高校和科研机构合作。','博士','不限','1. 计算机/AI相关专业博士\n2. 在NLP/CV/ML领域有高水平论文发表\n3. 精通PyTorch/TensorFlow\n4. 有大模型训练和优化经验优先\n5. 对学术研究充满热情',80000,40000,'active','NLP,AI研究,深度学习,大模型,论文','AI研究员（NLP方向）','2026-05-16 18:34:57.000000',0),(25,'杭州市余杭区文一西路969号','杭州',3,'2026-05-16 18:34:57.000000','负责阿里云控制台前端架构设计；制定前端技术规范和标准；推动微前端、Serverless等新技术落地；带领前端团队技术成长。','本科','5-10年','1. 5年以上前端开发经验\n2. 精通React/Vue等框架原理\n3. 有大型前端项目架构经验\n4. 熟悉Node.js和前端工程化\n5. 了解微前端和Serverless架构',60000,35000,'active','前端架构,React,微前端,Node.js,Serverless','前端架构师','2026-05-16 18:34:57.000000',0),(26,'杭州市余杭区文一西路969号','杭州',3,'2026-05-16 18:34:57.000000','负责电商业务的用户增长策略制定和执行；设计增长实验（AB测试）并分析效果；优化用户转化漏斗；与产品、市场团队协作。','本科','2-5年','1. 有用户增长或运营经验\n2. 精通数据分析，熟练使用SQL\n3. 了解增长黑客方法论\n4. 有AB测试经验\n5. 有创意和执行能力',40000,20000,'active','用户增长,运营,AB测试,数据分析,增长黑客','用户增长运营','2026-05-16 18:34:57.000000',0),(27,'杭州市余杭区文一西路969号','杭州',3,'2026-05-16 18:34:57.000000','负责应用安全评估和代码审计；建立安全开发流程（SDLC）；进行安全培训和意识提升；安全漏洞的跟踪和修复验证。','本科','3-5年','1. 信息安全/计算机相关专业\n2. 熟悉OWASP Top 10和常见安全漏洞\n3. 掌握代码审计技能\n4. 了解DevSecOps理念\n5. 有CISSP/OSCP认证优先',50000,25000,'active','应用安全,代码审计,OWASP,SDLC,DevSecOps','安全工程师（应用安全）','2026-05-16 18:34:57.000000',0),(28,'杭州市余杭区文一西路969号','杭州',3,'2026-05-16 18:34:57.000000','负责平台商家的日常运营和维护；帮助商家提升店铺运营能力；策划商家活动和促销方案；收集商家反馈并推动产品改进。','大专','1-3年','1. 有电商平台运营经验\n2. 良好的沟通和服务意识\n3. 了解淘宝/天猫商家后台\n4. 有数据分析基础\n5. 执行力强，善于总结',28000,15000,'active','商家运营,电商运营,淘宝,商家服务,活动策划','商家运营专员','2026-05-16 18:34:57.000000',0),(29,'杭州市余杭区文一西路969号','杭州',3,'2026-05-16 18:34:57.000000','负责MySQL/PostgreSQL数据库的运维和管理；进行数据库性能优化和SQL调优；制定备份和容灾策略；参与数据库架构设计。','本科','3-5年','1. 精通MySQL/PostgreSQL\n2. 有大规模数据库集群管理经验\n3. 熟悉数据库备份恢复和主从复制\n4. 了解分布式数据库（TiDB/OceanBase）\n5. 有云数据库运维经验',45000,25000,'active','DBA,MySQL,PostgreSQL,性能优化,数据库运维','数据库管理员（DBA）','2026-05-16 18:34:57.000000',0),(30,'杭州市余杭区文一西路969号','杭州',3,'2026-05-16 18:34:57.000000','负责淘宝内容社区的内容策划和编辑；挖掘优质内容创作者并进行扶持；策划内容专题和话题活动；分析内容数据，优化内容策略。','本科','1-3年','1. 中文/新闻/传媒相关专业\n2. 有内容编辑或新媒体运营经验\n3. 文字功底扎实\n4. 对电商和消费趋势有敏感度\n5. 会基础图片和视频处理',22000,12000,'active','内容运营,编辑,内容策划,新媒体,电商内容','内容运营编辑','2026-05-16 18:34:57.000000',0),(31,'深圳市南山区科技园南区','深圳',4,'2026-05-16 18:34:57.000000','负责信贷核心系统的开发和维护；参与微服务架构的拆分和优化；编写单元测试和集成测试；参与技术方案评审。','本科','2-5年','1. 2年以上Java开发经验\n2. 熟悉Spring Boot和MyBatis\n3. 了解分布式事务和消息队列\n4. 有金融系统开发经验优先\n5. 良好的编码习惯',35000,18000,'active','Java,Spring Boot,微服务,金融科技,信贷','Java后端开发工程师','2026-05-16 18:34:57.000000',0),(32,'深圳市南山区科技园南区','深圳',4,'2026-05-16 18:34:57.000000','负责信贷风控模型的开发和迭代；进行特征工程和模型评估；与业务团队协作制定风控策略；跟踪模型线上表现并持续优化。','硕士','2-5年','1. 统计学/数学/计算机相关专业硕士及以上\n2. 精通Python和SQL\n3. 熟悉逻辑回归/XGBoost等模型\n4. 有信用评分卡开发经验\n5. 了解风控业务知识',50000,25000,'active','风控建模,Python,XGBoost,信用评分,特征工程','风控建模工程师','2026-05-16 18:34:57.000000',0),(33,'深圳市南山区科技园南区','深圳',4,'2026-05-16 18:34:57.000000','负责消费信贷产品的规划和设计；进行竞品调研和市场分析；制定产品定价和运营策略；监控产品数据和风险指标。','本科','3-5年','1. 有互联网金融产品经验\n2. 了解信贷业务和监管政策\n3. 数据分析能力强\n4. 优秀的逻辑思维和沟通能力\n5. 对用户体验有追求',40000,20000,'active','金融产品,消费信贷,产品设计,数据分析,风控','金融产品经理','2026-05-16 18:34:57.000000',0),(34,'深圳市南山区科技园南区','深圳',4,'2026-05-16 18:34:57.000000','负责CI/CD流水线的搭建和优化；管理Kubernetes集群和容器化部署；建设监控和日志系统；推动研发效能提升。','本科','2-4年','1. 熟悉Linux系统和Shell脚本\n2. 精通Docker和Kubernetes\n3. 了解Jenkins/GitLab CI\n4. 有Prometheus/Grafana搭建经验\n5. 了解Terraform等IaC工具',32000,18000,'active','DevOps,Kubernetes,Docker,CI/CD,监控','DevOps工程师','2026-05-28 21:34:00.193158',2),(35,'深圳市南山区科技园南区','深圳',4,'2026-05-16 18:34:57.000000','负责公司管理后台和H5页面开发；与后端协作完成接口对接；组件库的开发和维护；前端性能优化。','本科','2-4年','1. 精通Vue.js框架\n2. 熟悉Element UI/Ant Design\n3. 了解Webpack/Vite等构建工具\n4. 有移动端H5开发经验\n5. 良好的UI还原能力',30000,15000,'active','Vue.js,前端,H5,Element UI,管理后台','前端开发工程师','2026-05-28 14:29:36.355925',2),(36,'深圳市南山区科技园南区','深圳',4,'2026-05-16 18:34:57.000000','负责风控业务数据的分析和报表；监测核心风险指标的变化；支持风控策略的数据需求；撰写数据分析报告。','本科','1-3年','1. 精通SQL和Excel\n2. 会Python或R\n3. 了解信贷风控基本概念\n4. 有金融行业数据分析经验优先\n5. 细心严谨，数据敏感度高',30000,15000,'active','数据分析,风控,SQL,Python,金融','数据分析师（风控方向）','2026-05-28 00:47:17.973965',1),(37,'深圳市南山区科技园南区','深圳',4,'2026-05-16 18:34:57.000000','负责功能测试和回归测试；编写测试用例和测试报告；参与需求评审，提出风险点；使用测试工具提高效率。','本科','1-3年','1. 有软件测试经验\n2. 了解测试方法论和流程\n3. 熟悉接口测试工具（Postman/JMeter）\n4. 会基本SQL查询\n5. 有自动化测试经验优先',22000,12000,'active','功能测试,接口测试,Postman,SQL,测试用例','测试工程师','2026-05-16 18:34:57.000000',0),(38,'深圳市南山区科技园南区','深圳',4,'2026-05-16 18:34:57.000000','负责消费信贷业务的法律合规审查；跟踪金融监管政策变化；审核合同和法律文件；提供合规咨询和培训。','本科','2-4年','1. 法学专业，通过司法考试\n2. 了解互联网金融相关法规\n3. 有金融行业法务经验优先\n4. 细心负责，风险意识强\n5. 良好的文字表达能力',25000,15000,'active','法务,合规,互联网金融,司法考试,合同审核','法务合规专员','2026-05-16 18:34:57.000000',0),(39,'深圳市南山区科技园南区','深圳',4,'2026-05-16 18:34:57.000000','负责App和Web端产品的UI设计；参与设计规范的制定和维护；输出高保真原型和切图；与产品和开发协作保证设计还原。','本科','1-3年','1. 设计相关专业\n2. 精通Figma/Sketch\n3. 有金融类App设计经验优先\n4. 了解移动端设计规范\n5. 有良好的设计审美',22000,12000,'active','UI设计,Figma,App设计,设计规范,金融设计','UI设计师','2026-05-16 18:34:57.000000',0),(40,'深圳市南山区科技园南区','深圳',4,'2026-05-16 18:34:57.000000','负责金融合作渠道的拓展和维护；对接银行、支付机构等合作伙伴；推动商务合作方案的落地；跟踪合作数据，优化合作策略。','本科','3-5年','1. 3年以上商务拓展经验\n2. 有金融行业资源优先\n3. 优秀的商务谈判能力\n4. 目标导向，结果驱动\n5. 能适应出差',35000,18000,'active','商务拓展,BD,金融合作,谈判,渠道管理','商务拓展经理','2026-05-16 18:34:57.000000',0),(41,'上海市静安区南京西路1788号','上海',5,'2026-05-16 18:34:57.000000','负责短视频账号的整体运营和内容规划；管理运营团队，制定KPI；数据分析，优化内容策略；与品牌方对接商业合作。','本科','2-4年','1. 有短视频运营成功案例\n2. 熟悉抖音/快手/B站平台规则\n3. 有团队管理经验\n4. 数据分析和内容判断能力强\n5. 对流行文化有敏锐嗅觉',28000,15000,'active','短视频运营,抖音,内容策划,团队管理,数据分析','短视频运营主管','2026-05-16 18:34:57.000000',0),(42,'上海市静安区南京西路1788号','上海',5,'2026-05-16 18:34:57.000000','负责短视频的剪辑和后期制作；根据脚本进行创意剪辑；添加特效和字幕；管理视频素材库。','大专','1-3年','1. 精通Premiere/After Effects\n2. 了解DaVinci Resolve调色\n3. 有短视频剪辑经验\n4. 节奏感好，有创意\n5. 熟悉短视频平台热门风格',20000,10000,'active','视频剪辑,Premiere,After Effects,短视频,调色','视频剪辑师','2026-05-16 18:34:57.000000',0),(43,'上海市静安区南京西路1788号','上海',5,'2026-05-16 18:34:57.000000','负责直播间的日常运营和管理；策划直播活动和脚本；分析直播数据优化效果；对接主播和供应链。','大专','1-3年','1. 有直播运营经验\n2. 了解抖音/快手直播生态\n3. 沟通协调能力强\n4. 数据敏感，善于总结\n5. 能接受弹性工作时间',20000,10000,'active','直播运营,主播管理,抖音直播,活动策划,数据分析','直播运营专员','2026-05-16 18:34:57.000000',0),(44,'上海市静安区南京西路1788号','上海',5,'2026-05-16 18:34:57.000000','负责短视频内容的创意策划和脚本撰写；研究用户喜好和内容趋势；指导拍摄和剪辑；对内容数据负责。','本科','2-4年','1. 广告/影视/编导相关专业\n2. 有成功的短视频内容案例\n3. 创意能力强，脑洞大\n4. 了解热门梗和流行元素\n5. 有短视频拍摄经验优先',22000,12000,'active','编导,内容策划,短视频,脚本创作,创意','内容编导','2026-05-16 18:34:57.000000',0),(45,'上海市静安区南京西路1788号','上海',5,'2026-05-16 18:34:57.000000','负责社交媒体视觉素材的设计；制作海报、banner、封面等；品牌VI的维护和应用；配合内容团队完成设计需求。','大专','1-3年','1. 精通Photoshop/Illustrator\n2. 有社交媒体设计经验\n3. 排版和配色能力强\n4. 会简单动效设计优先\n5. 审美在线，出图快',18000,10000,'active','平面设计,Photoshop,Illustrator,社交媒体,视觉设计','平面设计师','2026-05-16 18:34:57.000000',0),(46,'上海市静安区南京西路1788号','上海',5,'2026-05-16 18:34:57.000000','负责微信公众号/小红书的日常运营；撰写推文和笔记；粉丝互动和维护；跟踪数据并优化内容。','大专','0-2年','1. 有新媒体运营经验\n2. 文字功底好\n3. 会基础排版和图片处理\n4. 了解社交媒体运营玩法\n5. 学习能力强，有责任心',15000,8000,'active','新媒体运营,微信公众号,小红书,内容创作,排版','新媒体运营专员','2026-05-16 18:34:57.000000',0),(47,'上海市静安区南京西路1788号','上海',5,'2026-05-16 18:34:57.000000','负责达人的签约和管理；为达人制定发展计划和内容方向；对接商业合作和品牌方；帮助达人提升影响力和收入。','本科','2-4年','1. 有MCN或艺人经纪经验\n2. 了解达人孵化全流程\n3. 优秀的沟通和谈判能力\n4. 有品牌资源优先\n5. 对达人经济有热情',25000,12000,'active','MCN,达人经纪,签约管理,商业合作,达人孵化','MCN经纪人','2026-05-16 18:34:57.000000',0),(48,'上海市静安区南京西路1788号','上海',5,'2026-05-16 18:34:57.000000','负责粉丝社群的日常运营和维护；策划社群活动提升活跃度；收集用户反馈和需求；协助内容团队进行用户调研。','大专','0-2年','1. 有社群运营经验\n2. 热情开朗，善于沟通\n3. 能处理用户投诉和问题\n4. 有活动策划能力\n5. 熟悉微信生态',13000,8000,'active','社群运营,用户运营,粉丝维护,活动策划,微信生态','社群运营专员','2026-05-16 18:34:57.000000',0),(49,'上海市静安区南京西路1788号','上海',5,'2026-05-16 18:34:57.000000','协助商务团队处理日常事务；整理合同和商务文件；安排会议和行程；跟进合作项目的执行进度。','大专','0-2年','1. 专业不限\n2. 熟练使用Office办公软件\n3. 细心负责，执行力强\n4. 良好的沟通能力\n5. 能承受工作压力',12000,7000,'active','商务助理,行政,合同管理,沟通协调,Office','商务助理','2026-05-16 18:34:57.000000',0),(50,'上海市静安区南京西路1788号','上海',5,'2026-05-16 18:34:57.000000','负责短视频和直播的拍摄工作；管理和维护摄影器材；参与创意讨论，提供拍摄建议；对拍摄素材进行初步处理。','大专','1-3年','1. 有短视频拍摄经验\n2. 熟悉单反/微单等设备\n3. 了解灯光布置\n4. 会基础视频剪辑\n5. 有审美能力，构图好',20000,10000,'active','摄影,摄像,短视频拍摄,灯光,器材管理','摄影师/摄像师','2026-05-16 18:34:57.000000',0);
/*!40000 ALTER TABLE `jobs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `messages`
--

DROP TABLE IF EXISTS `messages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `conversation_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `file_name` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `is_read` bit(1) NOT NULL,
  `message_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `receiver_id` bigint NOT NULL,
  `sender_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `messages`
--

LOCK TABLES `messages` WRITE;
/*!40000 ALTER TABLE `messages` DISABLE KEYS */;
INSERT INTO `messages` VALUES (1,'11','app_4','2026-05-28 20:12:16.519004',NULL,NULL,NULL,_binary '\0','text',1,8);
/*!40000 ALTER TABLE `messages` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `notifications`
--

DROP TABLE IF EXISTS `notifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notifications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_read` bit(1) NOT NULL,
  `related_id` bigint DEFAULT NULL,
  `type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `notifications`
--

LOCK TABLES `notifications` WRITE;
/*!40000 ALTER TABLE `notifications` DISABLE KEYS */;
/*!40000 ALTER TABLE `notifications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `posts`
--

DROP TABLE IF EXISTS `posts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `posts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `likes` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `posts`
--

LOCK TABLES `posts` WRITE;
/*!40000 ALTER TABLE `posts` DISABLE KEYS */;
/*!40000 ALTER TABLE `posts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `resumes`
--

DROP TABLE IF EXISTS `resumes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `resumes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attachment_path` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `birthday` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `education` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gender` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `major` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `project_experience` text COLLATE utf8mb4_unicode_ci,
  `real_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `school` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `self_introduction` varchar(2000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `skills` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `work_experience` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_bwy42hdh23n1jypl7y8hna8y7` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `resumes`
--

LOCK TABLES `resumes` WRITE;
/*!40000 ALTER TABLE `resumes` DISABLE KEYS */;
INSERT INTO `resumes` VALUES (1,NULL,NULL,'1998-05-15','2026-05-16 18:34:57.000000','本科','lily@email.com','女','计算机科学','13900139001','推推电商项目：主导商品推荐引擎开发，QPS提升30%','李丽','北京大学','5年Java开发经验，热爱技术，追求代码质量','Java,Spring Boot,MySQL,Redis,Kafka','active','2026-05-16 18:34:57.000000',7,'2020-2023 字节跳动 Java开发工程师\n负责电商后台系统开发，参与双11大促保障'),(2,NULL,NULL,'1997-08-22','2026-05-16 18:34:57.000000','本科','xiaoming@email.com','男','软件工程','13900139002','商家后台重构项目：采用微前端架构，页面加载速度提升50%','王小明','浙江大学','3年前端经验，追求极致的用户体验','React,TypeScript,Vue.js,Node.js,Webpack','active','2026-05-16 18:34:57.000000',8,'2021-2023 阿里巴巴 前端开发工程师\n参与淘宝商家后台重构，使用React+TypeScript'),(3,NULL,NULL,'1996-12-03','2026-05-16 18:34:57.000000','硕士','ahua@email.com','男','数据科学','13900139003','短视频推荐项目：基于Graph Neural Network的用户兴趣建模','刘德华','上海交通大学','4年AI算法经验，专注推荐系统和用户增长','Python,PyTorch,TensorFlow,SQL,Spark','active','2026-05-16 18:34:57.000000',9,'2020-2024 华为 AI算法工程师\n负责推荐系统算法优化，CTR提升15%'),(4,NULL,NULL,'1999-03-18','2026-05-16 18:34:57.000000','本科','jingjing@email.com','女','新闻传播','13900139004','穿搭账号孵化：单条视频播放量破1000万','张静','复旦大学','2年内容运营经验，擅长爆款内容的策划和制作','内容策划,短视频运营,数据分析,文案写作,社群运营','active','2026-05-16 18:34:57.000000',10,'2022-2024 小红书 内容运营\n运营时尚类账号，粉丝从0增长到50万'),(5,NULL,NULL,'1995-07-09','2026-05-16 18:34:57.000000','本科','dapeng@email.com','男','电子工程','13900139005','5G基带芯片项目：完成物理层协议栈的嵌入式实现','赵大鹏','华中科技大学','5年嵌入式经验，深入底层系统开发','C语言,Linux驱动,嵌入式系统,ARM,RTOS','active','2026-05-16 18:34:57.000000',11,'2019-2024 华为 嵌入式软件工程师\n参与5G基站基带芯片的驱动开发'),(6,NULL,NULL,'2000-01-25','2026-05-16 18:34:57.000000','本科','xiaofang@email.com','女','金融学','13900139006','信贷风控建模：使用XGBoost+LR模型，KS值达到0.45','陈小芳','中山大学','1年风控经验，对金融科技充满热情','Python,SQL,风控建模,XGBoost,数据分析','active','2026-05-16 18:34:57.000000',12,'2023-2024 招商银行 风控分析师\n开发信用评分卡模型，坏账率降低20%'),(7,NULL,NULL,'1988-11-30','2026-05-16 18:34:57.000000','硕士','laowang@email.com','男','工商管理','13900139007','电商直播项目：负责整体产品规划，日活用户突破5000万','王建国','清华大学','10年产品经验，擅长从0到1构建产品','产品规划,数据分析,项目管理,用户研究,团队管理','active','2026-05-16 18:34:57.000000',13,'2015-2024 字节跳动 产品总监\n从0到1搭建电商直播产品线，GMV破百亿'),(8,NULL,NULL,'2000-06-14','2026-05-16 18:34:57.000000','本科','meimei@email.com','女','数字媒体','13900139008','科普系列企划：打造IP人设，3个月涨粉80万','周美美','中国传媒大学','年轻有活力，对内容创作有无限热情','视频编导,内容策划,脚本创作,剪辑,创意','active','2026-05-16 18:34:57.000000',14,'2023-2024 B站 视频编导\n策划的知识科普系列全网播放量超2亿'),(9,NULL,NULL,'1992-02-28','2026-05-16 18:34:57.000000','本科','sanfeng@email.com','男','计算机科学','13900139009','容器化迁移项目：将传统部署迁移到Kubernetes，成本降低40%','张三丰','武汉大学','8年运维经验，精通云原生技术栈','Linux,Kubernetes,Docker,Prometheus,Python','active','2026-05-16 18:34:57.000000',15,'2016-2024 腾讯 高级运维工程师\n管理2000+服务器集群，全年可用性99.99%'),(10,NULL,NULL,'1997-09-12','2026-05-16 18:34:57.000000','本科','liying@email.com','女','市场营销','13900139010','明星联名营销：与顶流艺人合作，话题阅读量破10亿','赵丽颖','南京大学','3年品牌营销经验，擅长打造爆款营销事件','品牌营销,活动策划,社交媒体,数据分析,项目管理','active','2026-05-16 18:34:57.000000',16,'2021-2024 完美日记 品牌营销经理\n操盘双11营销活动，ROI达到1:5'),(11,NULL,NULL,'1996-04-05','2026-05-16 18:34:57.000000','硕士','bowen@email.com','男','人工智能','13900139011','大语言模型训练：基于Llama架构预训练10B参数模型','刘博文','中国科学院大学','3年NLP研究经验，追求学术和工程的结合','Python,PyTorch,NLP,大模型,论文写作','active','2026-05-16 18:34:57.000000',17,'2021-2024 商汤科技 NLP算法研究员\n发表3篇ACL论文，开发的大模型在多个benchmark上达到SOTA'),(12,NULL,NULL,'1998-12-20','2026-05-16 18:34:57.000000','大专','cuihua@email.com','女','会计电算化','13900139012','财务系统上线：推动公司使用金蝶云财务系统','王翠花','深圳职业技术学院','4年财务经验，做事细致认真','会计,财务软件,Excel,税务,成本核算','active','2026-05-16 18:34:57.000000',18,'2020-2024 某科技公司 财务专员\n负责日常账务处理和税务申报'),(13,NULL,NULL,'1995-06-18','2026-05-16 18:34:57.000000','本科','xiaoyao@email.com','男','通信工程','13900139013','5G NR测试项目：主导38.141-1协议测试用例开发','李逍遥','电子科技大学','6年通信测试经验，熟悉3GPP标准','5G测试,射频,频谱仪,Python,通信协议','active','2026-05-16 18:34:57.000000',19,'2018-2024 中兴通讯 5G测试工程师\n完成5G基站射频一致性测试，通过工信部认证'),(14,NULL,NULL,'2001-10-08','2026-05-16 18:34:57.000000','大专','chaoyue@email.com','女','影视表演','13900139014','个人IP打造：通过日更短视频+每周直播，年收入突破百万','杨超越','上海视觉艺术学院','年轻有活力的创作者，正在寻找更大的发展平台','短视频创作,直播带货,表演,粉丝运营,个人IP','active','2026-05-16 18:34:57.000000',20,'2024-至今 全职达人\n抖音粉丝120万，直播间最高同时在线10万人'),(15,NULL,NULL,'1985-03-01','2026-05-16 18:34:57.000000','博士','xuesen@email.com','男','计算机科学','13900139015','Google Cloud AI平台：从0搭建，服务全球数百万开发者','钱学森','麻省理工学院','15年技术经验，拥有30+专利，追求技术创新','AI,云计算,分布式系统,技术管理,C++','active','2026-05-16 18:34:57.000000',21,'2010-2024 谷歌 技术总监\n领导Google Cloud AI团队，多个产品月活过亿');
/*!40000 ALTER TABLE `resumes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `remember_token` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`),
  KEY `idx_remember_token` (`remember_token`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-05-16 18:34:57.000000','$2a$10$k5BfSDAQis4DFcrdradn3u/ufHGwRHOycVANcx3rgCCzVYb39vNr2','2026-05-28 22:37:49.144508','boss_zhang',NULL,'张总','boss',NULL,NULL),(2,'2026-05-16 18:34:57.000000','$2a$10$j8MkMvoKH33d25hfufRb9e725RtqD9gntNRuD9qyz37s3YowAzDgq','2026-05-28 22:37:49.145515','boss_li',NULL,'李总','boss',NULL,NULL),(3,'2026-05-16 18:34:57.000000','$2a$10$BUVy2EmeCHGmhBwp6FCVE.EuAvCmlMPGPCoyZJnBlgXyWch.Ots7G','2026-05-28 22:37:49.145515','boss_wang',NULL,'王总','boss',NULL,NULL),(4,'2026-05-16 18:34:57.000000','$2a$10$BcCWkJEfV5S/t7VY8Ja17./QV8jtIpmXt0hwVKn/AmdpRcRQ2.ruC','2026-05-28 22:37:49.145515','boss_zhao',NULL,'赵总','boss',NULL,NULL),(5,'2026-05-16 18:34:57.000000','$2a$10$7dJZ3asZ.es48R309qjpYeSAmQog.21.SNW14GSmQbfXgL3fJuHCe','2026-05-28 22:37:49.145515','boss_chen',NULL,'陈总','boss',NULL,NULL),(6,'2026-05-16 18:34:57.000000','$2a$10$pBrhQmrTijjq1Axf7d083O7ew9vJkOMryinR8lfNr3zYB7kbdu5MC','2026-05-28 22:37:49.145515','seeker_lily',NULL,'莉莉','seeker',NULL,NULL),(7,'2026-05-16 18:34:57.000000','$2a$10$t8VHEAjfUxPwsi70EoXuYeFt25xSNo2DpCeJpSZA5hN1d.uXSuY4S','2026-05-28 22:37:49.145515','seeker_xiaoming',NULL,'小明','seeker',NULL,NULL),(8,'2026-05-16 18:34:57.000000','$2a$10$J026wteATH8MpeL.62Jut.dt6OKSxrlw6DMwTtjwOkHJTnLGjn/SS','2026-05-28 22:46:46.308161','seeker_ahua',NULL,'阿华','seeker','ea364176-b31f-4923-85e8-67b396083218',NULL),(9,'2026-05-16 18:34:57.000000','$2a$10$wyibo0kywLVYgKsXS899dOOElIyS2.2fwX/K4IqaseAmZBLBGayhO','2026-05-28 22:37:49.145515','seeker_jingjing',NULL,'静静','seeker',NULL,NULL),(10,'2026-05-16 18:34:57.000000','$2a$10$CtuoT1FEX2BE.x/8OrKXiert9XVfaJ6AHpm/q53ZIZ/3P9WkLcVsK','2026-05-28 22:37:49.146507','seeker_dapeng',NULL,'大鹏','seeker',NULL,NULL),(11,'2026-05-16 18:34:57.000000','$2a$10$/.bKNwS1a0bJ2ov3ednkqe/cYL/6lFbJBK2V6ouv4g.a.vvMRG1.2','2026-05-28 22:37:49.146507','seeker_xiaofang',NULL,'小芳','seeker',NULL,NULL),(12,'2026-05-16 18:34:57.000000','$2a$10$mTrT34fMkSmv6Bv12CXl0.7sCFYyIeFXRQFlq.lkZXkL7AH8masj.','2026-05-28 22:37:49.146507','seeker_laowang',NULL,'老王','seeker',NULL,NULL),(13,'2026-05-16 18:34:57.000000','$2a$10$opVQtBznG93gw/n.AIZjDusG7lFKC3bDgyLoTg2of/X7csUyj5cPS','2026-05-28 22:37:49.146507','seeker_meimei',NULL,'美美','seeker',NULL,NULL),(14,'2026-05-27 14:29:44.425679','$2a$10$61VsrZ4itVQm3Z6wrdMgy.q2e6qepkS2lCiqLH8IcpdbdtgwuFrh6','2026-05-28 22:37:49.146507','tester',NULL,'tester','seeker','b108f180-3fc3-443c-9c6b-98c7d79e61b9',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-28 23:22:20
