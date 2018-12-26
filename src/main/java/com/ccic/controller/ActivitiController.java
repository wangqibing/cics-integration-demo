package com.ccic.controller;

import com.ccic.common.ControllerDep;
import com.ccic.domain.CodeTable;
import com.ccic.domain.QSchedule;
import com.ccic.domain.UserInfo;
import com.ccic.dto.QSchedulePageRequestDto;
import com.ccic.service.CodeTableService;
import com.ccic.service.QScheduleService;
import com.ccic.service.UserInfoService;
import com.ccic.service.activiti.impl.ActivityDemoServiceImpl;
import com.ccic.util.DataParseUtil;
import com.ccic.vo.CodeVo;
import com.ccic.vo.ResultVo;
import com.ccic.vo.ScheduleVo;
import org.activiti.engine.*;
import org.activiti.engine.history.*;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.task.Task;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by 8000600758 on 2018/10/16.
 */
@RestController
@RequestMapping(value = "/activiti")
public class ActivitiController extends ControllerDep{
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private ActivityDemoServiceImpl activityDemoServiceImpl;

    @Autowired
    private RepositoryService repositoryService;//historyService

    @Autowired
    private HistoryService historyService;

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private QScheduleService qScheduleService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private  ProcessEngine processEngine;

    //通过登录人员查询工作任务
    @Transactional
    @PostMapping(value = "/worklist" )
    public ResultVo workListSearch(@RequestBody String userId) {
        try{
            //查询用户的任务列表
            List<Task> taskList = processEngine.getTaskService()//
                    .createTaskQuery()//
                    .taskCandidateOrAssigned(userId)
                    .list();
            List taskIdLists = taskList.stream().map(e->taskService.getVariable(e.getId(),"bizNo")).collect(Collectors.toList());
            taskIdLists = taskIdLists.size()==0?null:taskIdLists;
            Page<QSchedule> qSchedules = qScheduleService.findByIdPage(taskIdLists, PageRequest.of(0,10));
            List<ScheduleVo> questionVos = DataParseUtil.qSchedules2questionvos(qSchedules.getContent());
            Long totalElements =  qSchedules.getTotalElements();
            Integer totalPage = qSchedules.getTotalPages();
            Integer currentPage = qSchedules.getNumber();
            Map pageMap = new HashMap();
            pageMap.put("questionList",questionVos);
            pageMap.put("totalElements",totalElements);
            pageMap.put("totalPage",totalPage==null?0:totalPage);
            pageMap.put("currentPage",currentPage==null?0:currentPage+1);
            successResponse.setData(pageMap);
        }catch(Exception e){
            errorResponse.setData(e.getMessage());
            return errorResponse;
        }
        return successResponse;
    }


    //启动流程---215001
    @GetMapping(value = "/deploy" )
    public void deploy() {
        activityDemoServiceImpl.depolyProcesses();
    }

    //启动流程---215001
    @GetMapping(value = "/startPro" )
    public void startPro() {
        activityDemoServiceImpl.startProcesses("123456");
    }

    //获取受理员任务列表
    @GetMapping(value = "/findTasksForSL" )
    public void findTasksForSL() {
        List<Task> lists = activityDemoServiceImpl.findTasksByUserId("张三丰");
        System.out.println("任务列表："+lists);//任务列表：[Task[id=210028, name=受理], Task[id=215005, name=受理]]
    }

    //审批员通过审核
    @GetMapping(value = "/completeTasksForSL" )
    public void completeTasksForSL(@RequestParam String taskID) {
        activityDemoServiceImpl.completeTask(taskID, "张三丰", "true");//受理后，任务列表数据减少
    }




    //设置流程变量
    //设置流程变量【基本类型】
    @GetMapping(value = "/setTasksVar" )
    public void setTasksVar() {
        List<Task> lists = activityDemoServiceImpl.findTasksByUserId("sly1");
        for(Task task : lists) {//不知为何，变量保存成功，但数据表只有请假天数含有任务id，单获取流程变量时，根据任务id均可获取到（如下一测试）
            taskService.setVariable(task.getId(), "请假人", "sly1");
            taskService.setVariableLocal(task.getId(), "请假天数",3);
            taskService.setVariable(task.getId(), "请假日期", new Date());
        }
    }

    //获取流程变量
    @GetMapping(value = "/getTasksVar" )
    public void getTasksVar() {
        List<Task> lists = activityDemoServiceImpl.findTasksByUserId("sly1");
        for(Task task : lists) {
            //获取流程变量【基本类型】
            String person = (String) taskService.getVariable(task.getId(), "请假人");
            Integer day = (Integer) taskService.getVariableLocal(task.getId(), "请假天数");
            Date date = (Date) taskService.getVariable(task.getId(), "请假日期");

            System.out.println("流程变量："+person+"||"+day+"||"+date+"||");
        }
    }

    //设置流程变量【实体】
    @GetMapping(value = "/setTasksVarEntity" )
    public void setTasksVarEntity() {
        List<Task> lists = activityDemoServiceImpl.findTasksByUserId("sly1");
        for(Task task : lists) {
            Person p = new Person();
            p.setName("翠花");
            p.setId("20");
            p.setDate(null);;
            p.setNote("回去探亲，一起吃个饭123");
            taskService.setVariable(task.getId(), "人员信息(添加固定版本)", p);

            System.out.println("设置流程变量成功！");
        }
    }

    //获取流程变量【实体】  实体必须序列化
    @GetMapping(value = "/getTasksVarEntity" )
    public void getTasksVarEntity() {
        List<Task> lists = activityDemoServiceImpl.findTasksByUserId("sly1");
        for(Task task : lists) {
            // 2.获取流程变量，使用javaBean类型
            Person p = (Person)taskService.getVariable(task.getId(), "人员信息(添加固定版本)");
            System.out.println(" 请假人：  "+p.getName()+"  请假天数：  "+p.getId()+"   请假时间："+ p.getDate()+ "   请假原因： "+p.getNote());
        }
    }


    //生成流程图---232501
    @GetMapping(value = "/queryProImg" )
    public void queryProImg() throws Exception {
        activityDemoServiceImpl.queryProImg("232501");
    }

    //生成流程图（高亮）---232501
    @GetMapping(value = "/queryProHighLighted" )
    public void queryProHighLighted() throws Exception {
//        activityDemoServiceImpl.queryProHighLighted("232501");
    }

    /**
     * 查询流程变量的历史表,可以根据变量名称查询该变量的所有历史信息
     */
    @GetMapping(value = "/findHistoryProcessVariables" )
    public void findHistoryProcessVariables(){
        List<HistoricVariableInstance> list = historyService.createHistoricVariableInstanceQuery()//创建一个历史的流程变量查询对象
                .variableName("请假天数")
                .list();
        if (list!=null &&list.size()>0) {
            for (HistoricVariableInstance hvi : list) {
                System.out.println(hvi.getId()+"     "+hvi.getProcessInstanceId()+"   "+hvi.getVariableName()
                        +"   "+hvi.getVariableTypeName()+"    "+hvi.getValue());
                System.out.println("########################################");
            }
        }

    }


    /**
     *  历史流程实例查询
     *  http://blog.csdn.net/luckyzhoustar/article/details/48652783
     */
    @GetMapping(value = "/findHistoricProcessInstance" )
    public void findHistoricProcessInstance() {
        // 查询已完成的流程
        List<HistoricProcessInstance> datas = historyService
                .createHistoricProcessInstanceQuery().finished().list();
        System.out.println("使用finished方法：" + datas.size());
        // 根据流程定义ID查询
        datas = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionId("processDefinitionId").list();
        System.out.println("使用processDefinitionId方法： " + datas.size());
        // 根据流程定义key（流程描述文件的process节点id属性）查询
        datas = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("processDefinitionKey").list();
        System.out.println("使用processDefinitionKey方法： " + datas.size());
        // 根据业务主键查询
        datas = historyService.createHistoricProcessInstanceQuery()
                .processInstanceBusinessKey("processInstanceBusinessKey").list();
        System.out.println("使用processInstanceBusinessKey方法： " + datas.size());
        // 根据流程实例ID查询
        datas = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId("processInstanceId").list();
        System.out.println("使用processInstanceId方法： " + datas.size());
        // 查询没有完成的流程实例
        historyService.createHistoricProcessInstanceQuery().unfinished().list();
        System.out.println("使用unfinished方法： " + datas.size());
    }

    /**
     *  历史任务查询
     * @throws ParseException
     */
    @GetMapping(value = "/findHistoricTasks" )
    public void findHistoricTasks() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //历史数据查询
        List<HistoricTaskInstance> datas = historyService.createHistoricTaskInstanceQuery()
                .finished().list();
        System.out.println("使用finished方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionId("processDefinitionId").list();
        System.out.println("使用processDefinitionId方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey("testProcess").list();
        System.out.println("使用processDefinitionKey方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionName("testProcess2").list();
        System.out.println("使用processDefinitionName方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .processFinished().list();
        System.out.println("使用processFinished方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId("processInstanceId").list();
        System.out.println("使用processInstanceId方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .processUnfinished().list();
        System.out.println("使用processUnfinished方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee("crazyit").list();
        System.out.println("使用taskAssignee方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .taskAssigneeLike("%zy%").list();
        System.out.println("使用taskAssigneeLike方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .taskDefinitionKey("usertask1").list();
        System.out.println("使用taskDefinitionKey方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .taskDueAfter(sdf.parse("2020-10-11 06:00:00")).list();
        System.out.println("使用taskDueAfter方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .taskDueBefore(sdf.parse("2022-10-11 06:00:00")).list();
        System.out.println("使用taskDueBefore方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .taskDueDate(sdf.parse("2020-10-11 06:00:00")).list();
        System.out.println("使用taskDueDate方法查询：" + datas.size());
        datas = historyService.createHistoricTaskInstanceQuery()
                .unfinished().list();
        System.out.println("使用unfinished方法查询：" + datas.size());
    }
    /**
     *  历史行为查询
     *  流程在进行过程中，每每走一个节点，都会记录流程节点的信息，包括节点的id，名称、类型、时间等，保存到ACT_HI_ACTINST表中。
     * @throws ParseException
     */
    @GetMapping(value = "/findHistoricActivityInstance" )
    public void findHistoricActivityInstance() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //查询数据
        List<HistoricActivityInstance> datas = historyService.createHistoricActivityInstanceQuery()
                .activityId("endevent1").list();
        System.out.println("使用activityId查询：" + datas.size());
        datas = historyService.createHistoricActivityInstanceQuery()
                .activityInstanceId(datas.get(0).getId()).list();
        System.out.println("使用activityInstanceId查询：" + datas.size());
        datas = historyService.createHistoricActivityInstanceQuery()
                .activityType("intermediateSignalCatch").list();
        System.out.println("使用activityType查询：" + datas.size());
        datas = historyService.createHistoricActivityInstanceQuery()
                .executionId("executionId").list();
        System.out.println("使用executionId查询：" + datas.size());
        datas = historyService.createHistoricActivityInstanceQuery().finished().list();
        System.out.println("使用finished查询：" + datas.size());
        datas = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId("processInstanceId").list();
        System.out.println("使用processInstanceId查询：" + datas.size());
        datas = historyService.createHistoricActivityInstanceQuery()
                .taskAssignee("crazyit").list();
        System.out.println("使用taskAssignee查询：" + datas.size());
        datas = historyService.createHistoricActivityInstanceQuery().unfinished().list();
        System.out.println("使用unfinished查询：" + datas.size());
    }

    /**
     *  历史流程明细查询
     *  在流程进行的过程中，会产生许多明细数据，只有将History设置为最高级别的时候，才会被记录到ACT_HI_DETAIL表中。
     * @throws ParseException
     */
    @GetMapping(value = "/findHistoricDetail" )
    public void findHistoricDetail() {
        // 查询历史行为
        HistoricActivityInstance act = historyService.createHistoricActivityInstanceQuery()
                .activityName("First Task").finished().singleResult();
        List<HistoricDetail> datas = historyService.createHistoricDetailQuery()
                .activityInstanceId(act.getId()).list();
        System.out.println("使用activityInstanceId方法查询：" + datas.size());
        datas = historyService.createHistoricDetailQuery().excludeTaskDetails().list();
        System.out.println("使用excludeTaskDetails方法查询：" + datas.size());
        datas = historyService.createHistoricDetailQuery().formProperties().list();
        System.out.println("使用formProperties方法查询：" + datas.size());
        datas = historyService.createHistoricDetailQuery().processInstanceId("processInstanceId").list();
        System.out.println("使用processInstanceId方法查询：" + datas.size());
        datas = historyService.createHistoricDetailQuery().taskId("taskId").list();
        System.out.println("使用taskId方法查询：" + datas.size());
        datas = historyService.createHistoricDetailQuery().variableUpdates().list();
        System.out.println("使用variableUpdates方法查询：" + datas.size());
    }
}
