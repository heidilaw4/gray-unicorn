from django.db import models

from modelcluster.fields import ParentalKey

from wagtail.core.models import Page, Orderable
from wagtail.core.fields import RichTextField
from wagtail.admin.edit_handlers import FieldPanel, InlinePanel
from wagtail.images.edit_handlers import ImageChooserPanel

class CarouselItem(Orderable):
    image = models.ForeignKey(
        'wagtailimages.Image',
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name='+'
    )
    caption = models.CharField(max_length=255, blank=True)
    page = ParentalKey('HomePage', related_name='carousel_items')

    panels = [
        ImageChooserPanel('image'),
        FieldPanel('caption'),
    ]

class HomePage(Page):

    hero_image = models.ImageField(null=True, blank=True)
    hero_title = models.CharField(max_length=255, null=True, blank=True)
    hero_subtitle = models.CharField(max_length=255, null=True, blank=True)
    body = RichTextField(blank=True)

    content_panels = Page.content_panels + [
        FieldPanel('hero_image'),
        FieldPanel('hero_title'),
        FieldPanel('hero_subtitle'),
        FieldPanel('body', classname="full"),
        InlinePanel('carousel_items', label="Carousel Items"),
    ]
