package com.piedpiper.navi;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.List;

public class RendererRunnable implements Runnable{
    private static final int UPDATE_PERIOD_MS = 1000;

    private Context context;
    private ArFragment arFragment;
    private Handler handler;
    private final String TAG = RendererRunnable.class.getSimpleName();

    RendererRunnable(Context context, Handler handler, ArFragment arFragment) {
        this.context = context;
        this.arFragment = arFragment;
        this.handler = handler;
    }

    @Override
    public void run() {
        Log.d(TAG, "Adding object");
        addObject(Uri.parse("andy.sfb"));
        this.handler.postDelayed(this, UPDATE_PERIOD_MS);
    }

    /**
     * Try to get the plane in front and test a hit
     * Add the object if hit is successful
     */
    private void addObject(Uri objectUri) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        Point point = getScreenCenter();
        if (frame != null) {
            try {
                List<HitResult> hitResultList = frame.hitTest(point.x, point.y);
                for (HitResult hitResult : hitResultList) {
                    Trackable trackable = hitResult.getTrackable();
                    if ((trackable instanceof Plane &&
                            ((Plane) trackable).isPoseInPolygon(hitResult.getHitPose()))) {
                        placeObject(hitResult.createAnchor(), objectUri);
                        break;
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "Cannot find a plane");
            }
        }
    }

    /**
     * Place the object at an anchor
     */
    private void placeObject(Anchor anchor, Uri objectUri) {
        ((Activity) this.context).runOnUiThread(() -> ModelRenderable.builder()
                .setSource(RendererRunnable.this.context, objectUri)
                .build()
                .thenAccept(renderable -> addNodeToScene(anchor, renderable))
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(RendererRunnable.this.context, "Unable to load renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        }));
    }

    /**
     * Add a renderable to an anchor
     */
    private void addNodeToScene(Anchor anchor, Renderable renderable) {
        Log.d(TAG, "adding node");
        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        node.select();
    }

    /**
     * Get center of the screen
     */
    private android.graphics.Point getScreenCenter() {
        View view = ((Activity) this.context).findViewById(android.R.id.content);
        return new android.graphics.Point(view.getWidth() / 2, view.getHeight() / 2);
    }

}
