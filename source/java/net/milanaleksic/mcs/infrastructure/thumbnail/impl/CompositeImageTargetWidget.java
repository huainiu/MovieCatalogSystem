package net.milanaleksic.mcs.infrastructure.thumbnail.impl;

import com.google.common.base.Optional;
import net.milanaleksic.mcs.application.gui.helper.ShowImageComposite;
import org.eclipse.swt.graphics.Image;

import javax.annotation.Nullable;

/**
 * User: Milan Aleksic
 * Date: 3/17/12
 * Time: 1:58 PM
 */
class CompositeImageTargetWidget implements ImageTargetWidget {

    private final ShowImageComposite composite;
    private final Optional<String> imdbId;

    public CompositeImageTargetWidget(ShowImageComposite composite, @Nullable String imdbId) {
        this.composite = composite;
        this.imdbId = Optional.fromNullable(imdbId);
    }

    public Optional<String> getImdbId() {
        return imdbId;
    }

    @Override
    public void safeSetImage(Optional<Image> image, String imdbId) {
        if (composite.isDisposed())
            return;
        if (!this.imdbId.isPresent() || !this.imdbId.get().equals(imdbId))
            return;
        setImage(image.orNull());
    }

    @Override
    public void setImage(Image image) {
        composite.setImage(Optional.of(image));
    }

}
