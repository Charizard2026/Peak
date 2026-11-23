package com.carter.peak

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class PeakGameView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var w = 1f; private var h = 1f; private var last = 0L
    private var started = false; private var won = false
    private var camera = 0f
    private val worldH = 3000f
    private var stamina = 100f
    private var checkpoint = worldH - 130f
    private var x = 75f; private var y = worldH - 145f
    private var vx = 0f; private var vy = 0f
    private var facing = 1f; private var grounded = false; private var grabbing = false
    private var jumpLatch = false; private var invuln = 0f
    private var left = false; private var right = false; private var up = false
    private var down = false; private var grab = false; private var jump = false
    private val pw = 25f; private val ph = 38f
    private data class Ledge(val x:Float,val y:Float,val width:Float,val rest:Boolean=false,val summit:Boolean=false)
    private data class Rock(val x:Float,val y:Float,val r:Float,val phase:Float)
    private data class Dust(var x:Float,var y:Float,var vx:Float,var vy:Float,var life:Float)
    private val ledges = mutableListOf<Ledge>()
    private val rocks = mutableListOf<Rock>()
    private val dust = mutableListOf<Dust>()
    private val buttons = mutableMapOf<String,RectF>()
    private var banner = "FIND YOUR PEAK"

    init { isFocusable = true; buildWorld() }
    private fun buildWorld() {
        ledges.clear(); rocks.clear()
        ledges.add(Ledge(0f, worldH-35f, 1200f))
        var lx=85f; var ly=worldH-100f
        repeat(35) { i ->
            lx=(lx + if(i%2==0) 145f else -125f).coerceIn(20f,850f)
            ly-=Random.nextFloat()*27f+65f
            ledges.add(Ledge(lx,ly,Random.nextFloat()*65f+115f,i%6==4))
            if(i%5==3) rocks.add(Rock((lx+45f).coerceIn(15f,950f),ly-23f,13f,Random.nextFloat()*6f))
        }
        ledges.add(Ledge(350f,65f,300f,false,true))
    }
    override fun onSizeChanged(width:Int,height:Int,oldw:Int,oldh:Int) {
        w=width.toFloat(); h=height.toFloat(); reset()
    }
    private fun reset() {
        x=75f; y=worldH-145f; vx=0f; vy=0f; stamina=100f
        checkpoint=y; camera=(worldH-h*.7f).coerceAtLeast(0f); won=false
        banner="FIND YOUR PEAK"
    }
    override fun onDraw(c:Canvas) {
        val now=System.nanoTime()
        val dt=if(last==0L) 0f else ((now-last)/1e9).toFloat().coerceIn(0f,.033f)
        last=now
        if(started&&!won) update(dt)
        drawGame(c)
        postInvalidateOnAnimation()
    }
    private fun update(dt:Float) {
        val wasGrounded=grounded
        val move=(if(right)1f else 0f)-(if(left)1f else 0f)
        if(move!=0f){ facing=move; vx+=move*(if(wasGrounded)850f else 500f)*dt }
        else vx*=if(wasGrounded).78f else .96f
        vx=vx.coerceIn(-220f,220f)
        val near=ledges.any { y+ph>it.y-25f && y<it.y+25f &&
            abs(x+pw/2-(it.x+it.width/2))<it.width/2+pw/2+10f }
        grabbing=grab&&near&&stamina>0&&!wasGrounded
        if(grabbing){
            vx*=.7f; vy=when { up->-118f; down->105f; else->0f }
            stamina-=(if(up||down)13f else 7f)*dt
        } else {
            vy+=720f*dt
            if(up&&near&&stamina>0){vy=-115f;stamina-=10f*dt}
        }
        if(jump&&!jumpLatch&&(wasGrounded||grabbing)){
            vy=if(grabbing)-310f else -345f; vx+=facing*95f
            stamina=(stamina-5f).coerceAtLeast(0f); jumpLatch=true
            burst(x+pw/2,y+ph,7)
        }
        if(!jump)jumpLatch=false
        val oldBottom=y+ph
        x=(x+vx*dt).coerceIn(0f,w-pw); y+=vy*dt; grounded=false
        ledges.forEach { l ->
            if(vy>=0f&&oldBottom<=l.y+7f&&y+ph>=l.y&&x+pw>l.x+4f&&x<l.x+l.width-4f){
                y=l.y-ph;vy=0f;grounded=true
                if(l.rest)stamina=(stamina+24f*dt).coerceAtMost(100f)
                if(l.summit){won=true;banner="SUMMIT REACHED!"}
                if(l.y<checkpoint-420f){checkpoint=l.y-40f;banner="CHECKPOINT REACHED"}
            }
        }
        if(grounded)stamina=(stamina+7f*dt).coerceAtMost(100f)
        stamina=stamina.coerceIn(0f,100f); invuln=(invuln-dt).coerceAtLeast(0f)
        rocks.forEach { r ->
            val rx=r.x+sin(System.currentTimeMillis()/700.0+r.phase).toFloat()*18f
            val dx=x+pw/2-rx; val dy=y+ph/2-r.y
            if(invuln<=0f&&dx*dx+dy*dy<(r.r+15f).pow(2)) respawn()
        }
        if(y>worldH+150f)respawn()
        val target=(y-h*.47f).coerceIn(0f,(worldH-h).coerceAtLeast(0f))
        camera+=(target-camera)*min(1f,dt*4f)
        dust.forEach { it.x+=it.vx*dt;it.y+=it.vy*dt;it.vy+=240f*dt;it.life-=dt }
        dust.removeAll{it.life<=0}
    }
    private fun respawn(){x=75f;y=checkpoint;vx=0f;vy=0f;stamina=max(45f,stamina);invuln=1.2f;banner="KEEP CLIMBING";burst(x,y,12)}
    private fun burst(a:Float,b:Float,n:Int){repeat(n){dust.add(Dust(a,b,Random.nextFloat()*180-90,Random.nextFloat()*-140,.55f))}}
    private fun text(c:Canvas,s:String,tx:Float,ty:Float,size:Float,color:Int,bold:Boolean=true){
        paint.color=color;paint.textSize=size
        paint.typeface=Typeface.create("sans-serif",if(bold)Typeface.BOLD else Typeface.NORMAL)
        c.drawText(s,tx,ty,paint)
    }
    private fun drawGame(c:Canvas){
        paint.shader=LinearGradient(0f,0f,0f,h,0xff719eb6.toInt(),0xffeee3d0.toInt(),Shader.TileMode.CLAMP)
        c.drawRect(0f,0f,w,h,paint);paint.shader=null
        paint.color=0xff748f9a.toInt()
        val far=Path().apply{moveTo(0f,h*.7f);lineTo(w*.22f,h*.34f);lineTo(w*.42f,h*.58f);lineTo(w*.68f,h*.24f);lineTo(w,h*.62f);lineTo(w,h);lineTo(0f,h);close()}
        c.drawPath(far,paint)
        c.save();c.translate(0f,-camera)
        paint.color=0xff697b75.toInt()
        val mountain=Path().apply{moveTo(w*.05f,worldH+100);lineTo(w*.28f,worldH-750);lineTo(w*.48f,worldH-1500);lineTo(w*.72f,0f);lineTo(w*.98f,worldH+100);close()}
        c.drawPath(mountain,paint)
        paint.color=0xffeef2ea.toInt()
        val snow=Path().apply{moveTo(w*.62f,700f);lineTo(w*.72f,0f);lineTo(w*.82f,650f);lineTo(w*.76f,590f);lineTo(w*.72f,660f);lineTo(w*.68f,610f);close()}
        c.drawPath(snow,paint)
        ledges.forEach { l ->
            paint.color=when{l.summit->0xfff1ebdd.toInt();l.rest->0xff8ab7a0.toInt();else->0xff465956.toInt()}
            c.drawRect(l.x,l.y,l.x+l.width,l.y+14f,paint)
            paint.color=0xffd7e0d4.toInt();c.drawRect(l.x,l.y,l.x+l.width,l.y+4f,paint)
            if(l.rest){paint.color=0xffffd36c.toInt();c.drawCircle(l.x+l.width/2,l.y-7f,5f,paint)}
        }
        rocks.forEach { r ->
            val rx=r.x+sin(System.currentTimeMillis()/700.0+r.phase).toFloat()*18f
            paint.color=0xff343d42.toInt();c.drawCircle(rx,r.y,r.r,paint)
            paint.color=0xffaab2b0.toInt();c.drawCircle(rx-4,r.y-4,r.r*.27f,paint)
        }
        if(invuln<=0||System.currentTimeMillis()/100%2L==0L){
            paint.color=0xff263b4a.toInt();c.drawRect(x+4,y+13,x+21,y+36,paint)
            paint.color=0xffe7b58d.toInt();c.drawRoundRect(x+5,y,x+21,y+16,4f,4f,paint)
            paint.color=0xffe8a94f.toInt();c.drawRect(x+2,y+10,x+23,y+17,paint)
            paint.color=0xff17232b.toInt();c.drawCircle(x+if(facing>0)17f else 9f,y+7,2f,paint)
            paint.color=0xff25343a.toInt();c.drawRect(x+3,y+32,x+11,y+38,paint);c.drawRect(x+14,y+32,x+22,y+38,paint)
        }
        dust.forEach{paint.color=0xffffd36c.toInt();paint.alpha=(it.life/.55f*255).toInt().coerceIn(0,255);c.drawRect(it.x,it.y,it.x+4,it.y+4,paint)}
        paint.alpha=255;c.restore()
        drawHud(c)
        if(!started)drawStart(c)
        if(won)drawVictory(c)
    }
    private fun drawHud(c:Canvas){
        paint.color=0xcc101923.toInt();c.drawRoundRect(18f,16f,190f,68f,13f,13f,paint)
        text(c,"PEAK.",31f,43f,23f,0xffffffff.toInt());text(c,"SUMMIT RUN",32f,59f,9f,0xffb8c9d2.toInt())
        val bx=w-220f;paint.color=0xcc101923.toInt();c.drawRoundRect(bx,16f,w-18f,72f,13f,13f,paint)
        text(c,"STAMINA",bx+12,35f,10f,0xffffffff.toInt())
        paint.color=0xff43515a.toInt();c.drawRoundRect(bx+12,43f,w-30,52f,5f,5f,paint)
        paint.color=if(stamina<25)0xffff7777.toInt() else 0xffffd36c.toInt()
        c.drawRoundRect(bx+12,43f,bx+12+(w-bx-42)*stamina/100f,52f,5f,5f,paint)
        text(c,"SUMMIT ${(100*(1-y/worldH)).toInt().coerceIn(0,100)}%",bx+12,66f,9f,0xffffffff.toInt())
        if(started&&!won)text(c,banner,w/2-75f,105f,17f,0xffffffff.toInt())
        drawButton(c,"left",20f,h-78f,52f,"◀");drawButton(c,"right",78f,h-78f,52f,"▶")
        drawButton(c,"jump",w-75f,h-78f,55f,"JUMP");drawButton(c,"grab",w-137f,h-78f,55f,"GRAB")
        drawButton(c,"up",w-137f,h-139f,55f,"▲");drawButton(c,"down",w-137f,h-17f,55f,"▼")
    }
    private fun drawButton(c:Canvas,id:String,bx:Float,by:Float,size:Float,label:String){
        val r=RectF(bx,by,bx+size,by+size);buttons[id]=r
        val active=when(id){"left"->left;"right"->right;"up"->up;"down"->down;"jump"->jump;"grab"->grab;else->false}
        paint.color=if(active)0xffe8a94f.toInt() else 0xcc101923.toInt();c.drawRoundRect(r,12f,12f,paint)
        paint.color=0xffffffff.toInt();text(c,label,bx+if(label.length>2)7f else 17f,by+size/2+5f,if(label.length>2)10f else 19f,paint.color)
    }
    private fun drawStart(c:Canvas){
        paint.color=0xdd101923.toInt();c.drawRoundRect(w*.2f,h*.15f,w*.8f,h*.84f,22f,22f,paint)
        text(c,"PEAK",w*.28f,h*.34f,48f,0xffffd36c.toInt())
        text(c,"FIND YOUR SUMMIT",w*.28f,h*.42f,17f,0xffffffff.toInt())
        text(c,"Climb the ridge. Save your stamina.",w*.28f,h*.52f,14f,0xffd2dde5.toInt(),false)
        text(c,"Touch START to begin",w*.28f,h*.61f,15f,0xffffffff.toInt())
        val r=RectF(w*.28f,h*.69f,w*.53f,h*.8f);buttons["start"]=r
        paint.color=0xffffd36c.toInt();c.drawRoundRect(r,12f,12f,paint)
        text(c,"START",r.left+22,r.centerY()+6,18f,0xff15202a.toInt())
    }
    private fun drawVictory(c:Canvas){
        paint.color=0xdd101923.toInt();c.drawRoundRect(w*.2f,h*.23f,w*.8f,h*.77f,22f,22f,paint)
        text(c,"SUMMIT REACHED!",w*.29f,h*.43f,27f,0xffffd36c.toInt())
        text(c,"Tap anywhere to climb again",w*.32f,h*.59f,14f,0xffffffff.toInt(),false)
    }
    override fun onTouchEvent(e:MotionEvent):Boolean{
        val i=e.actionIndex;val tx=e.getX(i);val ty=e.getY(i)
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN->{
                if(!started){if(buttons["start"]?.contains(tx,ty)==true){started=true;reset()}}
                else if(won){reset()}
                else setControl(tx,ty,true)
            }
            MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP->{setControl(tx,ty,false);if(e.pointerCount<=1)clearControls()}
            MotionEvent.ACTION_CANCEL->clearControls()
        }
        return true
    }
    private fun setControl(tx:Float,ty:Float,value:Boolean){
        buttons.forEach{(id,r)->if(r.contains(tx,ty))when(id){
            "left"->left=value;"right"->right=value;"up"->up=value;"down"->down=value
            "jump"->jump=value;"grab"->grab=value
        }}
    }
    private fun clearControls(){left=false;right=false;up=false;down=false;jump=false;grab=false}
}
