package cn.lehome.dispatcher.quartz.service.invoke;

import cn.lehome.base.api.park.bean.statistics.CountTask;
import cn.lehome.base.api.park.bean.statistics.MadeCountSumData;
import cn.lehome.base.api.park.service.car.CarEnterExitApiService;
import cn.lehome.base.api.park.service.park.ParkPassagewayFacilityApiService;
import cn.lehome.base.api.park.service.statistics.CountTaskApiService;
import cn.lehome.bean.park.enums.car.ParkProperty;
import cn.lehome.dispatcher.quartz.service.AbstractInvokeServiceImpl;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 统计前一天零点到12点之间车场的进出数据 然后将统计数据同步到count_task表中
 * @Author: Sunwj@sqbj.com
 * @Date: 2020/2/24 2:27 下午
 */
@Service("statisticsParkEnterExitScheduleJobService")
public class StatisticsParkEnterExitScheduleJobServiceImpl extends AbstractInvokeServiceImpl {

    @Autowired
    private CountTaskApiService countTaskApiService;

    @Autowired
    private ParkPassagewayFacilityApiService parkPassagewayFacilityApiService;

    @Autowired
    private CarEnterExitApiService carEnterexitApiService;

    @Override
    public void doInvoke(Map<String, String> params) throws ParseException {
        logger.info("定时统计车场进出数据开始。。。。。。。");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date=new Date();
        Map<String, String> dataMap = startEndData(date);
        String startTime = dataMap.get("startTime");
        String endTime = dataMap.get("endTime");
        Date dayStart = sdf.parse(dataMap.get("dayStart"));

        List<CountTask> countTasksList = new ArrayList<>();
        //获取全部车场出入口id
        List<Integer> idList = parkPassagewayFacilityApiService.findAllId();
        if (CollectionUtils.isNotEmpty(idList)) {
            for (int i=0; i<idList.size(); i++) {
                List<Object[]> parkCountList = carEnterexitApiService.countSumByDateAndPassagewayId(idList.get(i), startTime, endTime);
                List<MadeCountSumData> madeCountSumDataList = new ArrayList<>();
                if (parkCountList != null) {
                    //循环获取相同areaId的记录，相同areaId表明一个是长停车一个是临停车
                    for (int j = 0; j < parkCountList.size(); j++) {
                        MadeCountSumData madeCountSumData = new MadeCountSumData();
                        madeCountSumData.setPassAgeWayId(Integer.parseInt(parkCountList.get(j)[0].toString()));
                        madeCountSumData.setParkProperty(ParkProperty.get(Integer.parseInt(parkCountList.get(j)[1].toString())));
                        madeCountSumData.setCountDate(parkCountList.get(j)[2].toString());
                        madeCountSumData.setCount(Integer.parseInt(parkCountList.get(j)[3].toString()));
                        madeCountSumData.setSum(Integer.parseInt(parkCountList.get(j)[4].toString()));
                        madeCountSumData.setAreaId(Long.parseLong(parkCountList.get(j)[5].toString()));
                        madeCountSumData.setTenantId(parkCountList.get(j)[6].toString());
                        madeCountSumDataList.add(madeCountSumData);
                    }
                    //获取list后进行数据组装
                    Map<Long, MadeCountSumData> longMap = new HashMap<>();
                    Map<Long, MadeCountSumData> shortMap = new HashMap<>();
                    SimpleDateFormat countSDF=new SimpleDateFormat("yyyy-MM-dd");
                    Set<Long> areaIdSet = new HashSet<Long>();
                    if (CollectionUtils.isNotEmpty(madeCountSumDataList)){
                        for (int j = 0; j < madeCountSumDataList.size(); j++) {
                            if (madeCountSumDataList.get(j).getParkProperty().getValue() == 0) {
                                longMap.put(madeCountSumDataList.get(j).getAreaId(),madeCountSumDataList.get(j));
                            } else {
                                shortMap.put(madeCountSumDataList.get(j).getAreaId(),madeCountSumDataList.get(j));
                            }
                            areaIdSet.add(madeCountSumDataList.get(j).getAreaId());
                        }
                        //将得到的数据封装到map中
                        Iterator<Long> iterator = areaIdSet.iterator();
                        while (iterator.hasNext()){
                            Long areaId = iterator.next();
                            CountTask countTask = new CountTask();
                            countTask.setAreaId(areaId);
                            countTask.setCountTime(countSDF.format(dayStart));
                            if (longMap.get(areaId) != null) {
                                countTask.setLongCarTotal(longMap.get(areaId).getCount());
                                countTask.setLongChargeTotal(new BigDecimal(longMap.get(areaId).getSum()));
                                if (countTask.getPassAgeWayId() == null) {
                                    countTask.setPassAgeWayId(longMap.get(areaId).getPassAgeWayId());
                                }
                                if (StringUtils.isEmpty(countTask.getTenantId())) {
                                    countTask.setTenantId(longMap.get(areaId).getTenantId());
                                }
                            }else {
                                countTask.setLongCarTotal(0);
                                countTask.setLongChargeTotal(new BigDecimal(0));
                            }
                            if (shortMap.get(areaId) != null) {
                                countTask.setShortCarTotal(shortMap.get(areaId).getCount());
                                countTask.setShortChargeTotal(new BigDecimal(shortMap.get(areaId).getSum()));
                                if (countTask.getPassAgeWayId() == null) {
                                    countTask.setPassAgeWayId(shortMap.get(areaId).getPassAgeWayId());
                                }
                                if (StringUtils.isEmpty(countTask.getTenantId())) {
                                    countTask.setTenantId(shortMap.get(areaId).getTenantId());
                                }
                            }else {
                                countTask.setShortCarTotal(0);
                                countTask.setShortChargeTotal(new BigDecimal(0));
                            }
                            countTask.setChargeTotal(countTask.getLongChargeTotal().add(new BigDecimal(countTask.getShortCarTotal())));
                            Integer longChargeNumTotal = carEnterexitApiService.countTotalPriceByPassagewayIdAndParkProperty(idList.get(i),0, startTime,endTime);
                            Integer shortChargeNumTotal = carEnterexitApiService.countTotalPriceByPassagewayIdAndParkProperty(idList.get(i),1, startTime,endTime);
                            countTask.setLongChargeNumTotal(longChargeNumTotal);
                            countTask.setShortChargeNumTotal(shortChargeNumTotal);
                            countTasksList.add(countTask);
                        }
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(countTasksList)) {
                for (int j = 0; j < countTasksList.size(); j++) {
                    countTaskApiService.creatByCountTask(countTasksList.get(j));
                }
            }
        }
    }


    private Map<String, String> startEndData(Date date){

        Map<String, String> dataMap = new HashMap<>();
        //获取当前时间的前一天时间,只需要具体到天
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        Date date=new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, -1);

        //一天的开始时间 yyyy:MM:dd 00:00:00
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        Date dayStart = calendar.getTime();
        dataMap.put("dayStart", dayStart.toString());
        String startTime = sdf.format(dayStart);
        dataMap.put("startTime", startTime);

        //一天的结束时间 yyyy:MM:dd 23:59:59
        calendar.set(Calendar.HOUR_OF_DAY,23);
        calendar.set(Calendar.MINUTE,59);
        calendar.set(Calendar.SECOND,59);
        calendar.set(Calendar.MILLISECOND,999);
        Date dayEnd = calendar.getTime();
        String endTime = sdf.format(dayEnd);
        dataMap.put("endTime", endTime);

        return dataMap;
    }

}
