/**
 * Assembly instance editor.
 *
 *
 *
 * Sample assembly spec generated by:com.softmotions.ncms.adm.AsmEditorRS
 *
 *            {
 *                "id" : 3,
 *                "name" : "pub.content",
 *                "type" : null,
 *                "description" : null,
 *                "options" : null,
 *                "parentRefs" : ["2:pub.main"],
 *                "core" : null,
 *                "effectiveCore" : {
 *                    "id" : 1,
 *                    "location" : "foo/bar",
 *                    "name" : "fobarcore",
 *                    "templateEngine" : null
 *                },
 *                "effectiveAttributes" : [
 *                    {
 *                        "asmId" : 1,
 *                        "name" : "copyright",
 *                        "type" : "string",
 *                        "value" : "My company (c)",
 *                        "options" : null,
 *                        "hasLargeValue" : false
 *                    },
 *                    {
 *                        "asmId" : 1,
 *                        "name" : "title",
 *                        "type" : "string",
 *                        "value" : "Hello world",
 *                        "options" : null,
 *                        "hasLargeValue" : false
 *                    },
 *                    {
 *                        "asmId" : 3,
 *                        "name" : "content",
 *                        "type" : "string",
 *                        "value" : "Simple text",
 *                        "options" : null,
 *                        "hasLargeValue" : false
 *                    }
 *                ]
 *            }
 *
 * @asset(ncms/icon/16/actions/core_link.png)
 */
qx.Class.define("ncms.asm.AsmEditor", {
    extend : qx.ui.core.Widget,

    statics : {
    },

    events : {
    },

    properties : {

        /**
         * Assembly ID to load in editor
         */
        "asmId" : {
            apply : "__applyAsmId",
            nullable : true,
            check : "Number"
        },

        /**
         * Set assembly JSON representation
         * ofcom.softmotions.ncms.asm.Asm
         */
        "asmSpec" : {
            apply : "__applyAsmSpec",
            nullable : true,
            check : "Object"
        }
    },

    construct : function() {
        this.base(arguments);
        this._setLayout(new qx.ui.layout.VBox());

        var form = this.__form = new qx.ui.form.Form();
        var vmgr = form.getValidationManager();
        vmgr.setRequiredFieldMessage(this.tr("This field is required"));

        var el = new qx.ui.form.TextField();
        el.setRequired(true);
        form.add(el, this.tr("Name"), null, "name");

        el = new qx.ui.form.TextField();
        form.add(el, this.tr("Description"), null, "description");

        el = new sm.ui.form.ButtonField(null, "ncms/icon/16/actions/core_link.png");
        el.setReadOnly(true);
        form.add(el, this.tr("Core"), null, "core");

        el = new ncms.asm.AsmParentsTable();
        el.addListener("parentsChanged", function() {
            this.__reload("parents");
        }, this);
        form.add(el, this.tr("Parents"), null, "parents");

        el = new ncms.asm.AsmAttrsTable();
        el.addListener("attributesChanged", function() {
            this.__reload("attributes");
        }, this);
        form.add(el, this.tr("Attributes"), null, "attributes");

        var fr = new sm.ui.form.FlexFormRenderer(form);
        fr.setAppearance("ncms-wsa-form");
        this._add(fr);
    },

    members : {

        __form : null,


        __applyAsmId : function(value, old) {
            if (value == null) {
                this.setAsmSpec(null);
                return;
            }
            this.__reload();
        },

        __applyAsmSpec : function(spec) {
            if (spec == null) {
                this.__form.reset();
                return;
            }
            var ctls = this.__form.getItems();

            if (spec._part == null) {
                ctls["name"].setValue(spec["name"]);
                ctls["description"].setValue(spec["description"]);
            }
            if (spec["effectiveCore"] != null) {
                var ecore = spec["effectiveCore"];
                var ecoreVal = ecore["location"];
                if (ecore["templateEngine"] != null) {
                    ecoreVal += (";" + ecore["templateEngine"]);
                }
                ctls["core"].setValue(ecoreVal);
            }
            ctls["parents"].setAsmSpec(spec);
            ctls["attributes"].setAsmSpec(spec);
        },

        __reload : function(part) {
            var req = new sm.io.Request(
                    ncms.Application.ACT.getRestUrl("asms", {id : this.getAsmId()}),
                    "GET", "application/json");
            req.send(function(resp) {
                var asmSpec = resp.getContent();
                asmSpec._part = part;
                this.setAsmSpec(asmSpec);
                asmSpec._part = null;
            }, this);
        }
    },

    destruct : function() {
        this.__form = null;
    }
});