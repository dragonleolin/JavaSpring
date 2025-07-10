package com.example.autoDemo.util;

import com.example.autoDemo.data.StockResponse;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ChartUtil {

    public static byte[] generateLineChartImage(String code, List<StockResponse> stockList) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 圖片產生的筆數保留最後 20 筆資料
        List<StockResponse> limitedList =stockList.stream().sorted((a, b) -> b.getMarketTime().compareTo(a.getMarketTime()))// 按時間新到舊排序
                .limit(20)
                .sorted((a, b) -> a.getMarketTime().compareTo(b.getMarketTime())) // 重新按時間舊到新
                .toList();

        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;

        for (StockResponse stock : limitedList) {
            try {
                double price = Double.parseDouble(stock.getPrice());
                dataset.addValue(price, "price", stock.getMarketTime());
                // 計算最小與最大價格
                if (price < minPrice) minPrice = price;
                if (price > maxPrice) maxPrice = price;
            } catch (NumberFormatException ignored) {}
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "股票價格走勢 - " + code,
                "time", "price",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        chart.getTitle().setFont(new Font("SansSerif", Font.BOLD, 16));

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.DARK_GRAY);
        plot.setRangeGridlinePaint(Color.DARK_GRAY);

        // Y 軸價格區間設定為 ±20 範圍
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        if (minPrice != Double.MAX_VALUE && maxPrice != Double.MIN_VALUE) {
            double lower = minPrice - 20;
            double upper = maxPrice + 20;
            rangeAxis.setRange(lower, upper);
        }

        // X 軸標籤轉向與顯示優化
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        domainAxis.setTickLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        // 折線與節點設定
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, true); // 顯示節點
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));
        renderer.setDefaultItemLabelsVisible(true); // 顯示標籤
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelFont(new Font("SansSerif", Font.PLAIN, 10));

        plot.setRenderer(renderer);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(out, chart, 700, 400); // 適合手機寬度
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成圖表失敗", e);
        }
    }

    public static byte[] generatePieChart(Map<String, Long> data, String title) {
        // 只取前 10 名查詢最多的
        Map<String, Long> top10 = data.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                        (Map.Entry<String, Long> e) -> e.getKey(),
                        (Map.Entry<String, Long> e) -> e.getValue(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        top10.forEach((code, count) -> dataset.setValue(code + " (" + count + "次)", count));

        // 關閉圖例（或保留)
        JFreeChart chart = ChartFactory.createPieChart(
                title, dataset,
                false, // 設為 false 不顯示 legend
                true,
                false);

        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Microsoft JhengHei", Font.BOLD, 18));

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelFont(new Font("Microsoft JhengHei", Font.PLAIN, 12));
        plot.setCircular(true);
        plot.setLabelGap(0.02);
        plot.setBackgroundPaint(Color.WHITE);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(out, chart, 700, 500);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成圓餅圖失敗", e);
        }
    }



}