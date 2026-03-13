package com.airbnb.lottie.animation.content;

import android.annotation.TargetApi;
import android.graphics.Path;
import android.os.Build;
import com.airbnb.lottie.model.content.MergePaths;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/* JADX INFO: loaded from: classes.dex */
@TargetApi(19)
public class MergePathsContent implements PathContent, GreedyContent {
    private final MergePaths mergePaths;
    private final String name;
    private final Path firstPath = new Path();
    private final Path remainderPath = new Path();
    private final Path path = new Path();
    private final List<PathContent> pathContents = new ArrayList();

    public MergePathsContent(MergePaths mergePaths) {
        if (Build.VERSION.SDK_INT < 19) {
            throw new IllegalStateException("Merge paths are not supported pre-KitKat.");
        }
        this.name = mergePaths.getName();
        this.mergePaths = mergePaths;
    }

    @Override // com.airbnb.lottie.animation.content.GreedyContent
    public void absorbContent(ListIterator<Content> listIterator) {
        while (listIterator.hasPrevious() && listIterator.previous() != this) {
        }
        while (listIterator.hasPrevious()) {
            Content contentPrevious = listIterator.previous();
            if (contentPrevious instanceof PathContent) {
                this.pathContents.add((PathContent) contentPrevious);
                listIterator.remove();
            }
        }
    }

    @Override // com.airbnb.lottie.animation.content.Content
    public void setContents(List<Content> list, List<Content> list2) {
        for (int i = 0; i < this.pathContents.size(); i++) {
            this.pathContents.get(i).setContents(list, list2);
        }
    }

    @Override // com.airbnb.lottie.animation.content.PathContent
    public Path getPath() {
        this.path.reset();
        switch (this.mergePaths.getMode()) {
            case Merge:
                addPaths();
                break;
            case Add:
                opFirstPathWithRest(Path.Op.UNION);
                break;
            case Subtract:
                opFirstPathWithRest(Path.Op.REVERSE_DIFFERENCE);
                break;
            case Intersect:
                opFirstPathWithRest(Path.Op.INTERSECT);
                break;
            case ExcludeIntersections:
                opFirstPathWithRest(Path.Op.XOR);
                break;
        }
        return this.path;
    }

    @Override // com.airbnb.lottie.animation.content.Content
    public String getName() {
        return this.name;
    }

    private void addPaths() {
        for (int i = 0; i < this.pathContents.size(); i++) {
            this.path.addPath(this.pathContents.get(i).getPath());
        }
    }

    @TargetApi(19)
    private void opFirstPathWithRest(Path.Op op) {
        this.remainderPath.reset();
        this.firstPath.reset();
        for (int size = this.pathContents.size() - 1; size >= 1; size--) {
            PathContent pathContent = this.pathContents.get(size);
            if (pathContent instanceof ContentGroup) {
                ContentGroup contentGroup = (ContentGroup) pathContent;
                List<PathContent> pathList = contentGroup.getPathList();
                for (int size2 = pathList.size() - 1; size2 >= 0; size2--) {
                    Path path = pathList.get(size2).getPath();
                    path.transform(contentGroup.getTransformationMatrix());
                    this.remainderPath.addPath(path);
                }
            } else {
                this.remainderPath.addPath(pathContent.getPath());
            }
        }
        PathContent pathContent2 = this.pathContents.get(0);
        if (pathContent2 instanceof ContentGroup) {
            ContentGroup contentGroup2 = (ContentGroup) pathContent2;
            List<PathContent> pathList2 = contentGroup2.getPathList();
            for (int i = 0; i < pathList2.size(); i++) {
                Path path2 = pathList2.get(i).getPath();
                path2.transform(contentGroup2.getTransformationMatrix());
                this.firstPath.addPath(path2);
            }
        } else {
            this.firstPath.set(pathContent2.getPath());
        }
        this.path.op(this.firstPath, this.remainderPath, op);
    }
}
