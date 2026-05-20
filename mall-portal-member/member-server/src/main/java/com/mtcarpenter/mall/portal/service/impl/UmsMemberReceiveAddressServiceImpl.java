package com.mtcarpenter.mall.portal.service.impl;

import com.mtcarpenter.mall.mapper.UmsMemberReceiveAddressMapper;
import com.mtcarpenter.mall.model.UmsMember;
import com.mtcarpenter.mall.model.UmsMemberReceiveAddress;
import com.mtcarpenter.mall.model.UmsMemberReceiveAddressExample;
import com.mtcarpenter.mall.portal.service.UmsMemberReceiveAddressService;
import com.mtcarpenter.mall.portal.service.UmsMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 用户地址管理Service实现类
 * Created by macro on 2018/8/28.
 */
@Service
public class UmsMemberReceiveAddressServiceImpl implements UmsMemberReceiveAddressService {
    @Autowired
    private UmsMemberService memberService;
    @Autowired
    private UmsMemberReceiveAddressMapper addressMapper;

    @Override
    public int add(UmsMemberReceiveAddress address) {
        UmsMember currentMember = memberService.getCurrentMember();
        address.setMemberId(currentMember.getId());
        return addressMapper.insert(address);
    }

    @Override
    public int delete(Long id) {
        UmsMember currentMember = memberService.getCurrentMember();
        UmsMemberReceiveAddressExample example = new UmsMemberReceiveAddressExample();
        example.createCriteria().andMemberIdEqualTo(currentMember.getId()).andIdEqualTo(id);
        return addressMapper.deleteByExample(example);
    }

    @Override
    public int update(Long id, UmsMemberReceiveAddress address) {
        address.setId(null);
        UmsMember currentMember = memberService.getCurrentMember();
        //构建更新对象
        UmsMemberReceiveAddressExample example = new UmsMemberReceiveAddressExample();
        //防御式编程 + 权限隔离，只有两个id对的上才会进行记录修改
        example.createCriteria().andMemberIdEqualTo(currentMember.getId()).andIdEqualTo(id);
        if (address.getDefaultStatus() == 1) {//如果用户修改了默认地址
            //先将原来的默认地址去除
            UmsMemberReceiveAddress record = new UmsMemberReceiveAddress();
            record.setDefaultStatus(0);
            UmsMemberReceiveAddressExample updateExample = new UmsMemberReceiveAddressExample();
            //构建能找到当前用户原默认地址的记录的example
            updateExample.createCriteria()
                    //不能使用传入的id字段，不安全
                    .andMemberIdEqualTo(currentMember.getId())
                    .andDefaultStatusEqualTo(1);
            //把原默认地址的默认标识去掉
            addressMapper.updateByExampleSelective(record, updateExample);
        }
        //修改传入的地址
        return addressMapper.updateByExampleSelective(address, example);
    }

    @Override
    public List<UmsMemberReceiveAddress> list(Long memberId) {
        UmsMember currentMember = memberService.getCurrentMember();
        if (memberId != null) {
            currentMember = memberService.getById(memberId);
        }
        UmsMemberReceiveAddressExample example = new UmsMemberReceiveAddressExample();
        example.createCriteria().andMemberIdEqualTo(currentMember.getId());
        return addressMapper.selectByExample(example);
    }

    @Override
    public UmsMemberReceiveAddress getItem(Long id) {
        UmsMember currentMember = memberService.getCurrentMember();
        UmsMemberReceiveAddressExample example = new UmsMemberReceiveAddressExample();
        example.createCriteria().andMemberIdEqualTo(currentMember.getId()).andIdEqualTo(id);
        List<UmsMemberReceiveAddress> addressList = addressMapper.selectByExample(example);
        if (!CollectionUtils.isEmpty(addressList)) {
            return addressList.get(0);
        }
        return null;
    }
}
