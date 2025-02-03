package com.bastion.inc;

public class Location implements Comparable<Location>{
    private int x;
    private int y;

    public Location(){
        this.x = 0;
        this.y = 0;
    }

    public Location(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getX(){
        return x;
    }

    public int getY(){
        return y;
    }

    public Location times(double scale){
        return new Location((int) Math.round(x * scale), (int) Math.round(y * scale));
    }

    public Location plus(Location other){
        return new Location(this.x + other.x, this.y + other.y);
    }

    public Location minus(Location other){
        return new Location(this.x - other.x, this.y - other.y);
    }

    @Override
    public int compareTo(Location location) {
        if(this.y > location.y){
            return 1;
        }else if(this.y < location.y){
            return -1;
        }else if(this.x > location.x){
            return 1;
        }else if(this.x < location.x){
            return -1;
        }else{
            return 0;
        }
    }

    @Override
    public String toString(){
        return "Location{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    @Override
    public boolean equals(Object o){
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return x == location.x && y == location.y;
    }

    @Override
    public int hashCode(){
        return 31 * x + y;
    }
}
