package com.yourfamily.pdf.secure_pdf_converter.core.redaction.word;

import java.util.*;
import com.yourfamily.pdf.secure_pdf_converter.core.redaction.RedactionPlan;

public class RedactionPlanMerger {

    public static List<RedactionPlan> merge(List<RedactionPlan> plans) {

        List<RedactionPlan> result = new ArrayList<>();

        for (RedactionPlan plan : plans) {

            boolean merged = false;

            for (int i = 0; i < result.size(); i++) {

                RedactionPlan r = result.get(i);

                if (r.pageIndex() == plan.pageIndex()
                        && r.shapeType() == plan.shapeType()
                        && overlaps(r, plan)) {

                    double x = Math.min(r.pdfX(), plan.pdfX());
                    double y = Math.min(r.pdfY(), plan.pdfY());

                    double right =
                            Math.max(r.pdfX() + r.pdfWidth(),
                                     plan.pdfX() + plan.pdfWidth());

                    double bottom =
                            Math.max(r.pdfY() + r.pdfHeight(),
                                     plan.pdfY() + plan.pdfHeight());

                    result.set(i, new RedactionPlan(
                            r.pageIndex(),
                            x,
                            y,
                            right - x,
                            bottom - y,
                            r.shapeType()
                    ));

                    merged = true;
                    break;
                }
            }

            if (!merged)
                result.add(plan);
        }

        return result;
    }

    private static boolean overlaps(RedactionPlan a, RedactionPlan b) {

        return a.pdfX() < b.pdfX() + b.pdfWidth()
                && a.pdfX() + a.pdfWidth() > b.pdfX()
                && a.pdfY() < b.pdfY() + b.pdfHeight()
                && a.pdfY() + a.pdfHeight() > b.pdfY();
    }
}