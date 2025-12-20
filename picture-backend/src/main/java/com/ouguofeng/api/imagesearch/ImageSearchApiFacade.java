package com.ouguofeng.api.imagesearch;

import com.ouguofeng.api.imagesearch.model.ImageSearchResult;
import com.ouguofeng.api.imagesearch.sub.GetImageFirstUrlApi;
import com.ouguofeng.api.imagesearch.sub.GetImageListApi;
import com.ouguofeng.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 以图搜图接口
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

}
