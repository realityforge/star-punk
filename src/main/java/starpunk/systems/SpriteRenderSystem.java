package starpunk.systems;

import com.artemis.Aspect;
import com.artemis.ComponentMapper;
import com.artemis.Entity;
import com.artemis.EntitySystem;
import com.artemis.annotations.Mapper;
import com.artemis.utils.Bag;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import starpunk.components.Position;
import starpunk.components.Sprite;

public final class SpriteRenderSystem
  extends EntitySystem
{
  @Mapper
  ComponentMapper<Position> _positionMapper;
  @Mapper
  ComponentMapper<Sprite> _spriteMapper;

  private HashMap<String, AtlasRegion> _regions;
  private SpriteBatch _batch;
  private OrthographicCamera _camera;

  private Bag<AtlasRegion> _regionsByEntity;
  private List<Entity> _sortedEntities;

  public SpriteRenderSystem( final OrthographicCamera camera )
  {
    super( Aspect.getAspectForAll( Position.class, Sprite.class ) );
    _camera = camera;
  }

  @Override
  protected void initialize()
  {
    _regions = new HashMap<String, AtlasRegion>();
    final TextureAtlas textureAtlas = new TextureAtlas( Gdx.files.internal( "target/assets/game" ) );
    for( AtlasRegion r : textureAtlas.getRegions() )
    {
      _regions.put( r.name, r );
    }
    _regionsByEntity = new Bag<AtlasRegion>();

    _batch = new SpriteBatch();

    _sortedEntities = new ArrayList<Entity>();
  }

  @Override
  protected void begin()
  {
    _batch.setProjectionMatrix( _camera.combined );
    _batch.begin();
  }

  @Override
  protected boolean checkProcessing()
  {
    return true;
  }


  @Override
  protected void processEntities( final ImmutableBag<Entity> entities )
  {
    for( final Entity entity : _sortedEntities )
    {
      process( entity );
    }
  }

  protected void process( final Entity e )
  {
    if( _positionMapper.has( e ) )
    {
      final Position position = _positionMapper.getSafe( e );
      final Sprite sprite = _spriteMapper.get( e );

      final AtlasRegion spriteRegion = _regionsByEntity.get( e.getId() );
      _batch.setColor( sprite.getR(), sprite.getG(), sprite.getB(), sprite.getA() );

      if( null != spriteRegion )
      {
        float posX = position.getX() - ( spriteRegion.getRegionWidth() / 2 * sprite.getScaleX() );
        float posY = position.getY() - ( spriteRegion.getRegionHeight() / 2 * sprite.getScaleY() );
        _batch.draw( spriteRegion,
                    posX,
                    posY,
                    0,
                    0,
                    spriteRegion.getRegionWidth(),
                    spriteRegion.getRegionHeight(),
                    sprite.getScaleX(),
                    sprite.getScaleY(),
                    sprite.getRotation() );
      }
    }
  }

  protected void end()
  {
    _batch.end();
  }

  @Override
  protected void inserted( final Entity e )
  {
    final Sprite sprite = _spriteMapper.get( e );
    _regionsByEntity.set( e.getId(), _regions.get( sprite.getName() ) );

    _sortedEntities.add( e );

    Collections.sort( _sortedEntities, new Comparator<Entity>()
    {
      @Override
      public int compare( Entity e1, Entity e2 )
      {
        final Sprite s1 = _spriteMapper.get( e1 );
        final Sprite s2 = _spriteMapper.get( e2 );
        return s1.getLayer().compareTo( s2.getLayer() );
      }
    } );
  }

  @Override
  protected void removed( final Entity e )
  {
    _regionsByEntity.set( e.getId(), null );
    _sortedEntities.remove( e );
  }
}