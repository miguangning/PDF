package com.artifex.mupdfdemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MuPDFReaderView extends ReaderView implements View.OnTouchListener {

	public enum Mode {Viewing, Selecting, Drawing}
	private final Context mContext;
	private boolean mLinksEnabled = false;
	private static boolean isLongClickModule = false;
	private Mode mMode = Mode.Viewing;
	private boolean tapDisabled = false;
	private int tapPageMargin;
	float  startX = 0;
	float  startY= 0;
	Timer timer = null;

	protected void onTapMainDocArea() {}
	protected void onDocMotion() {}
	protected void onHit(Hit item) {};

	public void setLinksEnabled(boolean b) {
		mLinksEnabled = b;
		resetupChildren();
	}

	public void setMode(Mode m) {
		mMode = m;
	}

	private void setup()
	{
		// Get the screen size etc to customise tap margins.
		// We calculate the size of 1 inch of the screen for tapping.
		// On some devices the dpi values returned are wrong, so we
		// sanity check it: we first restrict it so that we are never
		// less than 100 pixels (the smallest Android device screen
		// dimension I've seen is 480 pixels or so). Then we check
		// to ensure we are never more than 1/5 of the screen width.
		DisplayMetrics dm = new DisplayMetrics();
		WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(dm);
		tapPageMargin = (int)dm.xdpi;
		if (tapPageMargin < 100)
			tapPageMargin = 100;
		if (tapPageMargin > dm.widthPixels/5)
			tapPageMargin = dm.widthPixels/5;


		setOnTouchListener(this);
	}

	public MuPDFReaderView(Context context) {
		super(context);
		mContext = context;
		setup();
	}

	public MuPDFReaderView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
		setup();
	}

	public boolean onSingleTapUp(MotionEvent e) {
		LinkInfo link = null;

		if (mMode == Mode.Viewing && !tapDisabled) {
			MuPDFView pageView = (MuPDFView) getDisplayedView();
			Hit item = pageView.passClickEvent(e.getX(), e.getY());
			onHit(item);
			if (item == Hit.Nothing) {
				if (mLinksEnabled && pageView != null
				&& (link = pageView.hitLink(e.getX(), e.getY())) != null) {
					link.acceptVisitor(new LinkInfoVisitor() {
						@Override
						public void visitInternal(LinkInfoInternal li) {
							// Clicked on an internal (GoTo) link
							setDisplayedViewIndex(li.pageNumber);
						}

						@Override
						public void visitExternal(LinkInfoExternal li) {
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri
									.parse(li.url));
							intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
							mContext.startActivity(intent);
						}

						@Override
						public void visitRemote(LinkInfoRemote li) {
							// Clicked on a remote (GoToR) link
						}
					});
				} else if (e.getX() < tapPageMargin) {
					super.smartMoveBackwards();
				} else if (e.getX() > super.getWidth() - tapPageMargin) {
					super.smartMoveForwards();
				} else if (e.getY() < tapPageMargin) {
					super.smartMoveBackwards();
				} else if (e.getY() > super.getHeight() - tapPageMargin) {
					super.smartMoveForwards();
				} else {
					onTapMainDocArea();
				}
			}
		}
		return super.onSingleTapUp(e);
	}

	@Override
	public boolean onDown(MotionEvent e) {

		return super.onDown(e);
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		MuPDFView pageView = (MuPDFView)getDisplayedView();
		switch (mMode) {
		case Viewing:
			if (!tapDisabled)
				onDocMotion();

			return super.onScroll(e1, e2, distanceX, distanceY);
		case Selecting:
			if (pageView != null)
				pageView.selectText(e1.getX(), e1.getY(), e2.getX(), e2.getY());
			return true;
		default:
			return true;
		}
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		switch (mMode) {
		case Viewing:
			return super.onFling(e1, e2, velocityX, velocityY);
		default:
			return true;
		}
	}

	public boolean onScaleBegin(ScaleGestureDetector d) {
		// Disabled showing the buttons until next touch.
		// Not sure why this is needed, but without it
		// pinch zoom can make the buttons appear
		tapDisabled = true;
		return super.onScaleBegin(d);
	}

	public boolean onTouchEvent(MotionEvent event) {

		if ((event.getAction() & event.getActionMasked()) == MotionEvent.ACTION_DOWN)
		{
			tapDisabled = false;
		}

		return super.onTouchEvent(event);
	}

	private float mX, mY;

	private static final float TOUCH_TOLERANCE = 2;

	public void touch_start(float x, float y, String tag) {
		MuPDFView pdfView = (MuPDFView) getDisplayedView();

		if (pdfView != null) {
			pdfView.startDraw(x, y, tag);
		}
		mX = x;
		mY = y;
	}

	public void revokeDraw() {
		MuPDFView pdfView = (MuPDFView)getDisplayedView();
		if (pdfView != null) {
			pdfView.revokeDraw();
		}
	}

	protected void onChildSetup(int i, View v) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber == i)
			((MuPDFView) v).setSearchBoxes(SearchTaskResult.get().searchBoxes);
		else
			((MuPDFView) v).setSearchBoxes(null);

		((MuPDFView) v).setLinkHighlighting(mLinksEnabled);

		((MuPDFView) v).setChangeReporter(new Runnable() {
			public void run() {
				applyToChildren(new ViewMapper() {
					@Override
					void applyToView(View view) {
						((MuPDFView) view).update();
					}
				});
			}
		});
	}

	protected void onMoveToChild(int i) {
		if (SearchTaskResult.get() != null
				&& SearchTaskResult.get().pageNumber != i) {
			SearchTaskResult.set(null);
			resetupChildren();
		}
	}

	@Override
	protected void onMoveOffChild(int i) {
		View v = getView(i);
		if (v != null)
			((MuPDFView)v).deselectAnnotation();
	}

	protected void onSettle(View v) {
		// When the layout has settled ask the page to render
		// in HQ
		((MuPDFView) v).updateHq(false);
	}

	protected void onUnsettle(View v) {
		// When something changes making the previous settled view
		// no longer appropriate, tell the page to remove HQ
		((MuPDFView) v).removeHq();
	}

	@Override
	protected void onNotInUse(View v) {
		((MuPDFView) v).releaseResources();
	}

	@Override
	protected void onScaleChild(View v, Float scale) {
		((MuPDFView) v).setScale(scale);
	}

	public static final String X = "XValue";
	public static final String Y = "YValue";

	//获取图纸坐标
	public  Map<String, String> getDrawingXY(View view,MotionEvent ev){
		Map<String, String> getDrawingXYMap =  new HashMap<>();

		switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				startX = ev.getX();
				startY = ev.getY();
				timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						isLongClickModule = true;
					}
				}, 300);
				break;
			case MotionEvent.ACTION_MOVE:
				double deltaX = Math.sqrt((ev.getX() - startX) * (ev.getX() - startX) + (ev.getY() - startY) * (ev.getY() - startY));
				if (deltaX > 20 && timer != null) { // 移动大于20像素
					timer.cancel();
					timer = null;
				}
				if(isLongClickModule){
					float getFX = 1.0f * (super.scrollX + ev.getX() - 2*super.relativleLeft) / super.getDisplayedView().getWidth();
					float getFY = 1.0f * (super.scrollY + (view.getHeight() - ev.getY())) / super.getDisplayedView().getHeight();
					if(getFX >= 0 && getFX <= super.getWidth() ) {
						String getSX = String.valueOf(getFX);
						String getSY = String.valueOf(getFY);

						getDrawingXYMap.put(X, getSX);
						getDrawingXYMap.put(Y, getSY);
					}
					timer = null;
					isLongClickModule = false;
				}
				break;
			default:
				isLongClickModule = false;
				if ( timer != null) {
					timer.cancel();
					timer = null;
				}
		}
		return getDrawingXYMap;
	}

	//坐标反向展示
	public Map<String, String> getXY(float x, float y) {
		Map<String, String> getDrawingXYMap =  new HashMap<>();

		float drawingXInt =x * super.getDisplayedView().getWidth()  + 2*super.relativleLeft - super.scrollX ;

		float drawingYInt = MuPDFReaderView.this.getHeight() -  (y * super.getDisplayedView().getHeight()  - super.scrollY);

		String getSX = String.valueOf(drawingXInt);
		String getSY = String.valueOf(drawingYInt);

		getDrawingXYMap.put(X, getSX);
		getDrawingXYMap.put(Y, getSY);

		return getDrawingXYMap;
	}

	//标记点点击
	public void setOnMarkClickListener(PageView.OnMarkClickListener l) {
		PageView.setOnMarkClickListener(l);
	}

	//onDraw完成时调用
	public void setOnDrawFinishedListener(PageView.OnDrawFinishedListener listener) {
		PageView.setOnDrawFinishedListener(listener);
	}

	/****************最新添加*****************/
	private boolean isDrawMark = true;
	private OnDrawMarkFinishedListener onDrawMarkFinishedListener;
	public interface OnDrawMarkFinishedListener {
		void onFinish(Map<String, String> map, String tag);
	}

	public void setOnDrawMarkFinishListener(OnDrawMarkFinishedListener listener) {
		this.onDrawMarkFinishedListener = listener;
	}

	public boolean isDrawMark() {
		return isDrawMark;
	}

	public void setDrawMark(boolean drawMark) {
		isDrawMark = drawMark;
	}

	@Override
	public boolean onTouch(View view, MotionEvent motionEvent) {
		if (isDrawMark) {
			Map<String, String> map = getDrawingXY(view, motionEvent);
			String d = String.valueOf(map.get("XValue"));
			if (!d.equals("null")) {
				String tag = String.valueOf(System.currentTimeMillis());
				touch_start(motionEvent.getX(), motionEvent.getY(), tag);
				onDrawMarkFinishedListener.onFinish(map, tag);
			}
		}
		return false;
	}
}
