from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('scheduler', '0008_auditlog_user_lockout'),
    ]

    operations = [
        migrations.AddField(
            model_name='user',
            name='assigned_courses',
            field=models.ManyToManyField(
                blank=True,
                related_name='assigned_lecturers',
                to='scheduler.course',
                verbose_name='Atanmış Dersler',
            ),
        ),
    ]
